package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Function;

abstract sealed class AbstractSignal<T> implements Signal<T> permits
        MutableSignalImpl,
        MappedSignal,
        MapDistinctSignal,
        CombinedSignal,
        SwitchingSignal,
        MergingSignal,
        AsyncSignalImpl,
        KeyedSignalImpl.SyncPartition,
        PartitionHandle,
        TickingSignal,
        PacedSignal,
        CollectionSignal,
        AbstractKeyedSignal.Keys
{
    private static final BiPredicate<Object, Object> DEFAULT_SAME_VALUE = Objects::equals;

    private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();
    private final ReferenceQueue<Runnable> deadNodes = new ReferenceQueue<>();
    private final Object activationLock = new Object();
    private volatile boolean retired;
    // 只拦截同线程重入. 并发派发允许重叠, 跨线程反馈环由公开契约禁止.
    @Nullable private Thread dispatchingThread;

    abstract long version();

    protected void onActive() {
    }

    protected void onInactive() {
    }

    @Override
    @NotNull
    public Subscription onDirty(@NotNull Runnable listener) {
        return this.register(listener, null);
    }

    // 下游节点随订阅条目登记, 死订阅可以从下游一路清扫到上游.
    @NotNull
    final Subscription linkTo(@NotNull AbstractSignal<?> source, @NotNull Runnable listener) {
        return source.register(() -> {
            this.reapDeadEntries();
            listener.run();
        }, this);
    }

    // 一次连接多个固定上游, 中途失败时撤销已经建立的订阅.
    @NotNull
    final Subscription[] linkAll(AbstractSignal<?>[] sources, @NotNull Runnable listener) {
        Subscription[] subscriptions = new Subscription[sources.length];
        int linked = 0;
        try {
            for (int index = 0; index < sources.length; index++) {
                subscriptions[index] = this.linkTo(sources[index], listener);
                linked++;
            }
        } catch (RuntimeException | Error exception) {
            for (int index = linked - 1; index >= 0; index--) {
                subscriptions[index].close();
            }
            throw exception;
        }
        return subscriptions;
    }

    @NotNull
    private Subscription register(@NotNull Runnable callback, @Nullable AbstractSignal<?> downstream) {
        // 每次注册都创建独立节点, 无捕获 lambda 被 JVM 缓存时也能随凭证结束订阅.
        BindingNode node = new BindingNode(callback, downstream);
        Entry entry = new Entry(node);
        node.bindEntry(entry);
        synchronized (this.activationLock) {
            if (this.retired) {
                entry.close();
                return node;
            }
            this.entries.add(entry);
            if (this.entries.size() == 1) {
                try {
                    this.onActive();
                } catch (RuntimeException | Error exception) {
                    this.entries.remove(entry);
                    throw exception;
                }
            }
        }
        this.reapDeadEntries();
        return node;
    }

    private void unregister(@NotNull Entry entry) {
        synchronized (this.activationLock) {
            if (this.entries.remove(entry) && this.entries.isEmpty()) {
                this.onInactive();
            }
        }
    }

    // 批量清理凭证已经被回收的弱订阅.
    final void reapDeadEntries() {
        Reference<?> reference = this.deadNodes.poll();
        if (reference == null) return;
        do {
            if (reference instanceof NodeReference dead) {
                dead.entry.markClosed();
            }
        } while ((reference = this.deadNodes.poll()) != null);
        this.sweepClosed();
    }

    // 从下游向上清扫派生链, 截断失效的节点也能及时停用.
    final void reapDownstream() {
        for (Entry entry : this.entries) {
            if (entry.isClosed()) continue;
            if (entry.node.get() instanceof BindingNode node) {
                AbstractSignal<?> downstream = node.downstream;
                if (downstream != null) downstream.reapDownstream();
            }
        }
        this.reapDeadEntries();
    }

    protected final void notifyDirty() {
        if (this.retired || this.entries.isEmpty()) return;
        if (this.dispatchingThread == Thread.currentThread()) {
            throw new IllegalStateException("Reentrant invalidation: a listener invalidated this signal while it was still dispatching");
        }
        this.dispatchingThread = Thread.currentThread();
        try {
            boolean reap = false;
            for (Entry entry : this.entries) {
                if (entry.isClosed()) continue;
                boolean alive = true;
                try {
                    alive = entry.deliver();
                } catch (RuntimeException exception) {
                    CraftEngine.instance().logger().error("Failed to deliver a signal invalidation", exception);
                }
                if (!alive) {
                    reap |= entry.markClosed();
                }
            }
            if (reap) this.sweepClosed();
        } finally {
            this.dispatchingThread = null;
        }
    }

    private void sweepClosed() {
        synchronized (this.activationLock) {
            if (this.entries.removeIf(Entry::isClosed) && this.entries.isEmpty()) {
                this.onInactive();
            }
        }
    }

    @Override
    @NotNull
    public <R> Signal<R> map(@NotNull Function<? super T, ? extends R> mapper) {
        return new MappedSignal<>(this, mapper);
    }

    @Override
    @NotNull
    public <R> Signal<R> mapDistinct(@NotNull Function<? super T, ? extends R> mapper) {
        return this.mapDistinct(mapper, defaultSameValue());
    }

    @Override
    @NotNull
    public <R> Signal<R> mapDistinct(@NotNull Function<? super T, ? extends R> mapper, @NotNull BiPredicate<? super R, ? super R> sameValue) {
        return new MapDistinctSignal<>(this, mapper, sameValue);
    }

    @Override
    @NotNull
    public Signal<T> debounce(long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be positive: " + ticks);
        }
        return new DebounceSignal<>(this, ticks, Signals.tickDelayer());
    }

    @Override
    @NotNull
    public Signal<T> debounceMillis(long millis) {
        if (millis <= 0) {
            throw new IllegalArgumentException("millis must be positive: " + millis);
        }
        return new DebounceSignal<>(this, millis, Signals.millisDelayer());
    }

    @Override
    @NotNull
    public Signal<T> throttle(long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be positive: " + ticks);
        }
        return new ThrottleSignal<>(this, ticks, Signals.tickDelayer());
    }

    @Override
    @NotNull
    public Signal<T> throttleMillis(long millis) {
        if (millis <= 0) {
            throw new IllegalArgumentException("millis must be positive: " + millis);
        }
        return new ThrottleSignal<>(this, millis, Signals.millisDelayer());
    }

    final int entryCount() {
        return this.entries.size();
    }

    final boolean isRetired() {
        return this.retired;
    }

    // 终止来源并关闭全部订阅, 后续注册直接得到已经关闭的凭证.
    final void retire() {
        synchronized (this.activationLock) {
            if (this.retired) return;
            this.retired = true;
        }
        for (Entry entry : this.entries) {
            entry.markClosed();
        }
        try {
            this.sweepClosed();
        } catch (RuntimeException exception) {
            CraftEngine.instance().logger().error("Failed to close a signal subscription", exception);
        }
    }

    static BiPredicate<Object, Object> defaultSameValue() {
        return DEFAULT_SAME_VALUE;
    }

    // 自定义判等函数只接收两个非 null 值.
    static <V> boolean same(BiPredicate<? super V, ? super V> sameValue, V current, V candidate) {
        if (current == null || candidate == null) {
            return current == candidate;
        }
        return sameValue.test(current, candidate);
    }

    @SuppressWarnings("unchecked")
    static <T> AbstractSignal<T> require(@NotNull Signal<T> signal) {
        return (AbstractSignal<T>) signal;
    }

    static void closeAll(Subscription @Nullable [] subscriptions) {
        if (subscriptions == null) return;
        for (int index = 0; index < subscriptions.length; index++) {
            subscriptions[index].close();
        }
    }

    record Versioned<V>(V value, long version) {
    }

    private final class Entry implements Subscription {
        private final AtomicBoolean closed = new AtomicBoolean();
        private final NodeReference node;

        private Entry(@NotNull BindingNode node) {
            this.node = new NodeReference(node, this, AbstractSignal.this.deadNodes);
        }

        private boolean deliver() {
            @Nullable Runnable target = this.node.get();
            if (target == null) return false;
            target.run();
            return true;
        }

        @Override
        public boolean isClosed() {
            return this.closed.get();
        }

        @Override
        public void close() {
            if (this.markClosed()) {
                AbstractSignal.this.unregister(this);
            }
        }

        private boolean markClosed() {
            if (!this.closed.compareAndSet(false, true)) return false;
            if (this.node.get() instanceof BindingNode node) {
                node.detach();
            }
            return true;
        }
    }

    // 凭证强持用户回调, Signal 只弱持凭证节点.
    private static final class BindingNode implements Subscription, Runnable {
        @Nullable private volatile Runnable callback;
        @Nullable private volatile Subscription entry;
        @Nullable private volatile AbstractSignal<?> downstream;
        private volatile boolean closed;

        private BindingNode(@NotNull Runnable callback, @Nullable AbstractSignal<?> downstream) {
            this.callback = callback;
            this.downstream = downstream;
        }

        private void bindEntry(@NotNull Subscription entry) {
            this.entry = entry;
        }

        @Override
        public void run() {
            @Nullable Runnable target = this.callback;
            if (target != null) {
                target.run();
            }
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            @Nullable Subscription current = this.entry;
            if (current != null) {
                current.close();
            } else {
                this.detach();
            }
        }

        private void detach() {
            this.closed = true;
            this.callback = null;
            this.entry = null;
            this.downstream = null;
        }
    }

    private static final class NodeReference extends WeakReference<Runnable> {
        private final AbstractSignal<?>.Entry entry;

        private NodeReference(@NotNull Runnable node, @NotNull AbstractSignal<?>.Entry entry, @NotNull ReferenceQueue<? super Runnable> queue) {
            super(node, queue);
            this.entry = entry;
        }
    }
}
