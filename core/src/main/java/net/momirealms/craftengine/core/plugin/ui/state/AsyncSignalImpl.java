package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.ui.Subscription;
import net.momirealms.craftengine.core.util.ThrowableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

final class AsyncSignalImpl<T> extends AbstractSignal<T> implements AsyncSignal<T> {
    private static final int UNLOADED = 0;
    private static final int IDLE = 1;
    private static final int LOADING = 2;
    private static final int LOADING_DIRTY = 3;
    private static final int MAX_SCHEDULE_ATTEMPTS = 2;
    private static final long MILLIS_PER_TICK = 50L;

    private final Executor executor;
    private final Supplier<? extends T> loader;
    private final BiPredicate<? super T, ? super T> sameValue;
    private final AtomicReference<Versioned<T>> state;
    private final AtomicInteger loadState = new AtomicInteger(UNLOADED);

    @Nullable private Thread loadingThread;
    @Nullable private final PollingState polling;

    AsyncSignalImpl(T placeholder, Executor executor, Supplier<? extends T> loader, BiPredicate<? super T, ? super T> sameValue, @Nullable Polling polling) {
        this.executor = executor;
        this.loader = loader;
        this.sameValue = sameValue;
        this.state = new AtomicReference<>(new Versioned<>(placeholder, 0L));
        this.polling = polling == null ? null : new PollingState(polling);
    }

    // 调度首载. 后续分区来源也可以在第一次真实取用时调用.
    void scheduleInitialLoad() {
        if (this.loadState.compareAndSet(UNLOADED, LOADING)) {
            // 首载被拒后恢复 UNLOADED, 后续访问可以重新提交.
            this.scheduleLoad(UNLOADED);
        }
    }

    @Override
    public T get() {
        return this.state.get().value();
    }

    @Override
    long version() {
        return this.state.get().version();
    }

    @Override
    protected void onActive() {
        PollingState polling = this.polling;
        if (polling == null) return;
        long generation = ++polling.generation;
        polling.clockSubscription = this.linkTo(polling.settings.clock(), () -> this.onPollTick(polling, generation));
        try {
            // 空闲值超过一个周期时立即补载, 首载未提交或仍在执行时不叠加.
            if (this.loadState.get() == IDLE && System.nanoTime() - polling.lastCompletedNanos >= polling.settings.periodNanos()) {
                this.dirty();
            }
        } catch (RuntimeException | Error exception) {
            // 激活补载失败时撤销时钟订阅, 配合 register 回滚.
            polling.clockSubscription.close();
            polling.clockSubscription = null;
            throw exception;
        }
    }

    @Override
    protected void onInactive() {
        PollingState polling = this.polling;
        if (polling == null) return;
        assert polling.clockSubscription != null;
        polling.clockSubscription.close();
        polling.clockSubscription = null;
    }

    // 轮询到拍时先清理整条派生链, 值长期不变也能发现空链并经 onInactive 停表.
    private void onPollTick(PollingState polling, long generation) {
        // 上一激活段已经取出的迟到回调不能借用新一段订阅多跑 loader.
        if (generation != polling.generation) return;
        this.reapDownstream();
        if (this.entryCount() == 0) return;
        this.dirty();
    }

    @Override
    public void dirty() {
        if (this.loadingThread == Thread.currentThread()) {
            throw new IllegalStateException("Reentrant invalidation: the loader invalidated this signal while it was still running");
        }
        while (true) {
            if (this.isRetired()) return;
            int current = this.loadState.get();
            if (current == UNLOADED || current == IDLE) {
                if (this.loadState.compareAndSet(current, LOADING)) {
                    this.scheduleLoad(current);
                    return;
                }
            } else if (current == LOADING) {
                if (this.loadState.compareAndSet(LOADING, LOADING_DIRTY)) return;
            } else {
                return;
            }
        }
    }

    // 执行器拒绝时恢复原状态. 拒绝窗口内又登记失效时只补一次提交重试.
    private void scheduleLoad(int rollbackState) {
        RuntimeException failure = null;
        for (int attempt = 0; attempt < MAX_SCHEDULE_ATTEMPTS; attempt++) {
            RuntimeException rejection = this.submit();
            if (rejection == null) {
                if (failure != null) {
                    CraftEngine.instance().logger().error("Failed to schedule an async signal load", failure);
                }
                return;
            }
            failure = ThrowableUtils.combine(failure, rejection);
            if (!this.loadState.compareAndSet(LOADING_DIRTY, LOADING)) {
                break;
            }
        }
        this.loadState.set(rollbackState);
        CraftEngine.instance().logger().error("Failed to schedule an async signal load", failure);
    }

    @Nullable
    private RuntimeException submit() {
        try {
            this.executor.execute(this::load);
            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private void load() {
        if (this.isRetired()) return;

        boolean changed = false;
        RuntimeException failure = null;
        boolean pending;
        try {
            changed = this.publishValue(this.runLoader());
        } catch (RuntimeException exception) {
            failure = exception;
        } finally {
            // 先记录完成时刻再发布 IDLE, 激活方看到空闲时也能看到本次时间.
            PollingState polling = this.polling;
            if (polling != null) {
                polling.lastCompletedNanos = System.nanoTime();
            }
            pending = this.loadState.getAndUpdate(current -> current == LOADING_DIRTY ? LOADING : IDLE) == LOADING_DIRTY;
        }

        if (pending && !this.isRetired()) {
            this.scheduleLoad(IDLE);
        }
        if (changed) {
            try {
                this.notifyDirty();
            } catch (RuntimeException exception) {
                failure = ThrowableUtils.combine(failure, exception);
            }
        }
        // loader 与失效派发都在执行器线程, 失败在这一边界上报.
        if (failure != null) {
            CraftEngine.instance().logger().error("Failed to load an async signal value", failure);
        }
    }

    private T runLoader() {
        this.loadingThread = Thread.currentThread();
        try {
            return this.loader.get();
        } finally {
            this.loadingThread = null;
        }
    }

    private boolean publishValue(T value) {
        while (true) {
            Versioned<T> current = this.state.get();
            if (same(this.sameValue, current.value(), value)) return false;
            if (this.state.compareAndSet(current, new Versioned<>(value, current.version() + 1))) return true;
        }
    }

    // 每个轮询来源独立保存激活状态, Polling 设置可以由以后加入的 keyed 分区共享.
    private static final class PollingState {
        private final Polling settings;
        private volatile long lastCompletedNanos;
        @Nullable private Subscription clockSubscription;
        private volatile long generation;

        private PollingState(Polling settings) {
            this.settings = settings;
        }
    }

    // periodNanos 用于判断重新激活时是否需要补载.
    record Polling(AbstractSignal<Long> clock, long periodNanos) {

        @NotNull
        static Polling everyTicks(long periodTicks) {
            return new Polling(require(Signals.everyTicks(periodTicks)), TimeUnit.MILLISECONDS.toNanos(periodTicks * MILLIS_PER_TICK));
        }

        @NotNull
        static Polling everyMillis(long periodMillis) {
            return new Polling(require(Signals.everyMillis(periodMillis)), TimeUnit.MILLISECONDS.toNanos(periodMillis));
        }
    }
}
