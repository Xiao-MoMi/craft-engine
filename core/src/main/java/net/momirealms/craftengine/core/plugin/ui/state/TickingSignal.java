package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;

final class TickingSignal extends AbstractSignal<Long> {
    private final Ticker ticker;
    private final AtomicReference<Versioned<Long>> state = new AtomicReference<>(new Versioned<>(0L, 0L));
    private final WeakPeriodCache<Signal<Long>> periodic = new WeakPeriodCache<>();
    @Nullable private Ticker.Handle handle;

    TickingSignal(Ticker ticker) {
        this.ticker = ticker;
    }

    @Override
    public Long get() {
        return this.state.get().value();
    }

    @Override
    long version() {
        return this.state.get().version();
    }

    @Override
    protected void onActive() {
        // 回调携带本激活段的起点. 迟到任务仍使用所属段起点, 总值跨停表单调递增.
        long base = this.state.get().value();
        this.handle = this.ticker.start(tick -> this.onTick(base, tick));
    }

    @Override
    protected void onInactive() {
        this.handle.cancel();
        this.handle = null;
    }

    private void onTick(long base, long tick) {
        long total = base + tick;
        while (true) {
            Versioned<Long> current = this.state.get();
            // 旧段迟到值不得覆盖新段进度, 不同调度线程经 CAS 发布单调值.
            if (total <= current.value()) return;
            if (this.state.compareAndSet(current, new Versioned<>(total, current.version() + 1))) {
                break;
            }
        }
        this.notifyDirty();
    }

    // 同周期共享降频视图, 每 tick 的重算次数只随周期种类增长.
    @NotNull
    Signal<Long> every(long periodTicks) {
        return this.periodic.get(periodTicks, period -> this.mapDistinct(tick -> tick / period));
    }

    int periodicViewCount() {
        return this.periodic.size();
    }

    @NotNull
    static Ticker platformTicker() {
        return onTick -> {
            AtomicLong elapsed = new AtomicLong();
            SchedulerTask task = CraftEngine.instance().scheduler().platform().runRepeating(
                    () -> onTick.accept(elapsed.incrementAndGet()), 1L, 1L
            );
            return task::cancel;
        };
    }

    // CraftEngine 的定时线程只负责投递, 相邻拍可能由不同的异步工作线程执行.
    @NotNull
    static Ticker asyncMillisTicker(long periodMillis) {
        return onTick -> {
            AtomicLong elapsed = new AtomicLong();
            SchedulerTask task = CraftEngine.instance().scheduler().asyncRepeating(
                    () -> onTick.accept(elapsed.incrementAndGet()), periodMillis, periodMillis, TimeUnit.MILLISECONDS
            );
            return task::cancel;
        };
    }

    // 测试可以替换的周期调度入口.
    interface Ticker {

        @NotNull
        Handle start(@NotNull LongConsumer onTick);

        interface Handle {

            void cancel();
        }
    }
}
