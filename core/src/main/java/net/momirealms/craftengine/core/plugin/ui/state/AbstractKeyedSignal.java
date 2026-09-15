package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

abstract sealed class AbstractKeyedSignal<K, T, P extends AbstractSignal<T>> implements KeyedSignal<K, T> permits KeyedSignalImpl, AsyncKeyedSignalImpl {
    // key -> 分区状态. 主表只保留仍有分区的 key, 弱一致遍历允许 clear() 边走边删.
    private final ConcurrentHashMap<K, KeyState<K, T, P>> store = new ConcurrentHashMap<>();
    private final WeakHashMap<K, WeakReference<PartitionHandle<K, T>>> detached = new WeakHashMap<>();   // 暂存分区已删但仍被持有的句柄
    private final Object detachedLock = new Object();                                                    // 保护 detached 与 keys 的首次创建, 锁序固定为主表 compute 在外
    @Nullable private volatile Keys<K> keys;                                                             // 第一次 keys() 才建, 没建过时建行删行只多一次 volatile 读

    // 在对应 key 的 compute 中创建新分区
    abstract P createPartition(K key);

    abstract void dirtyPartition(P partition);

    PartitionHandle<K, T> createHandle(K key) {
        return new PartitionHandle<>(this, key);
    }

    // 取出或新建指定 key 的分区
    final P partition(@NotNull K key) {
        // 常规读取命中现有分区, 避开 compute 的桶锁
        KeyState<K, T, P> state = this.store.get(key);
        if (state != null) {
            this.afterPartitionAccess(state.partition);
            return state.partition;
        }
        // 补建分区时一并接回暂存在旁表里的句柄
        AtomicReference<P> resolved = new AtomicReference<>();
        boolean[] created = new boolean[1];
        this.store.compute(key, (k, existing) -> {
            if (existing != null) {
                resolved.set(existing.partition);
                return existing;
            }
            P fresh = this.createPartition(k);
            KeyState<K, T, P> target = new KeyState<>(fresh);
            PartitionHandle<K, T> handle = this.takeDetached(k);
            if (handle != null) {
                // 句柄完成换挂后再随 KeyState 发布
                handle.attach(fresh);
                target.handleRef = new WeakReference<>(handle);
            }
            resolved.set(fresh);
            created[0] = true;
            return target;
        });
        // compute 结束后再派发, 回调可以安全访问同一张分区表
        if (created[0]) {
            this.keysChanged();
        }
        P partition = resolved.get();
        this.afterPartitionAccess(partition);
        return partition;
    }

    /**
     * 分区被取用后的回调, 在取用返回之前执行.
     * <p>每次取用都会调用, 因此实现必须幂等.
     * <p>取用指的是读写这个分区的值, {@link #at} 只取句柄不算.
     */
    void afterPartitionAccess(P partition) {
    }

    @Override
    public T get(@NotNull K key) {
        return this.partition(key).get();
    }

    @Override
    @NotNull
    public PartitionHandle<K, T> at(@NotNull K key) {
        // 已有句柄仍跟着当前分区时直接返回, 换挂仍只发生在对应 key 的 compute 内
        KeyState<K, T, P> state = this.store.get(key);
        if (state != null) {
            PartitionHandle<K, T> live = this.liveHandle(state);
            if (live != null && live.isAttachedTo(state.partition)) {
                return live;
            }
        }
        // 句柄和分区可以先后出现, 补建与换挂在一次 compute 内完成
        AtomicReference<PartitionHandle<K, T>> resolvedHandle = new AtomicReference<>();
        boolean[] created = new boolean[1];
        this.store.compute(key, (k, existing) -> {
            created[0] = existing == null;
            KeyState<K, T, P> target = existing != null ? existing : new KeyState<>(this.createPartition(k));
            PartitionHandle<K, T> handle = this.liveHandle(target);
            if (handle == null) {
                // 优先接回分区删除后暂存的句柄
                handle = this.takeDetached(k);
                if (handle == null) {
                    handle = this.createHandle(k);
                }
                // 用 resolvedHandle 保活到 compute 返回, 防止新句柄在交给调用方前被回收
                target.handleRef = new WeakReference<>(handle);
            }
            handle.attach(target.partition);
            resolvedHandle.set(handle);
            return target;
        });
        if (created[0]) {
            this.keysChanged();
        }
        return resolvedHandle.get();
    }

    @Nullable
    private PartitionHandle<K, T> liveHandle(KeyState<K, T, P> state) {
        WeakReference<PartitionHandle<K, T>> reference = state.handleRef;
        return reference == null ? null : reference.get();
    }

    // 暂存分区已删但仍存活的句柄, 调用方持有对应 key 的 compute 锁
    private void parkDetached(PartitionHandle<K, T> handle) {
        synchronized (this.detachedLock) {
            // 旁表使用句柄持有的 key. WeakHashMap.put 不替换已有 key 对象, 先删除可让新条目跟随当前句柄的 key 寿命.
            this.detached.remove(handle.key());
            this.detached.put(handle.key(), new WeakReference<>(handle));
        }
    }

    // 从旁表取回句柄并摘掉条目, 调用方持有对应 key 的 compute 锁
    @Nullable
    private PartitionHandle<K, T> takeDetached(K key) {
        synchronized (this.detachedLock) {
            WeakReference<PartitionHandle<K, T>> reference = this.detached.remove(key);
            return reference == null ? null : reference.get();
        }
    }

    @Override
    public void dirty(@NotNull K key) {
        KeyState<K, T, P> state = this.store.get(key);
        if (state != null) {
            this.dirtyPartition(state.partition);
        }
    }

    @Override
    public void dirtyAll() {
        for (KeyState<K, T, P> state : this.store.values()) {
            this.dirtyPartition(state.partition);
        }
    }

    @Override
    public void remove(@NotNull K key) {
        if (this.removeRow(key)) {
            this.keysChanged();
        }
    }

    // 删除一个分区但不派发 keys 失效, 由批量调用方决定通知时机
    private boolean removeRow(K key) {
        boolean[] removed = new boolean[1];
        this.store.computeIfPresent(key, (ignored, state) -> {
            // 先终止旧分区, 再让稳定句柄脱离它
            state.partition.retire();
            PartitionHandle<K, T> handle = this.liveHandle(state);
            if (handle != null) {
                handle.onPartitionEvicted(state.partition);
                this.parkDetached(handle);
            }
            removed[0] = true;
            // 主表只保留仍有分区的条目
            return null;
        });
        return removed[0];
    }

    @Override
    @NotNull
    public Signal<Set<K>> keys() {
        Keys<K> current = this.keys;
        if (current != null) return current;
        synchronized (this.detachedLock) {
            if (this.keys == null) {
                this.keys = new Keys<>(this);
            }
            return this.keys;
        }
    }

    // 建行或删行后推进 keys 版本, 从未请求过 keys() 时只多一次 volatile 读
    private void keysChanged() {
        Keys<K> current = this.keys;
        if (current != null) {
            current.changed();
        }
    }

    final int partitionCount() {
        return this.store.size();
    }

    final int handleCount() {
        int count = 0;
        for (KeyState<K, T, P> state : this.store.values()) {
            if (this.liveHandle(state) != null) {
                count++;
            }
        }
        synchronized (this.detachedLock) {
            for (WeakReference<PartitionHandle<K, T>> reference : this.detached.values()) {
                if (reference.get() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void clear() {
        boolean removed = false;
        for (K key : this.store.keySet()) {
            removed |= this.removeRow(key);
        }
        if (removed) {
            this.keysChanged();
        }
    }

    // 一个 key 的分区与稳定句柄. 分区建好后才入表, 句柄引用只在对应 key 的 compute 中更新.
    static final class KeyState<K, T, P extends AbstractSignal<T>> {
        final P partition;                                          // 当前分区
        @Nullable volatile WeakReference<PartitionHandle<K, T>> handleRef;    // 句柄的弱引用, null 表示无人取过句柄

        KeyState(P partition) {
            this.partition = partition;
        }
    }

    // 有分区的 key, 拉取时从主表复制不可修改快照
    static final class Keys<K> extends AbstractSignal<Set<K>> {
        private final AbstractKeyedSignal<K, ?, ?> owner;
        private final AtomicLong version = new AtomicLong();
        @Nullable private volatile Versioned<Set<K>> cached;

        private Keys(AbstractKeyedSignal<K, ?, ?> owner) {
            this.owner = owner;
        }

        @Override
        public Set<K> get() {
            // 先读版本再复制. 并发建删会留下偏旧版本, 下一次拉取会重新复制.
            long version = this.version.get();
            Versioned<Set<K>> current = this.cached;
            if (current != null && current.version() == version) {
                return current.value();
            }
            Set<K> snapshot = Set.copyOf(this.owner.store.keySet());
            this.cached = new Versioned<>(snapshot, version);
            return snapshot;
        }

        @Override
        long version() {
            return this.version.get();
        }

        private void changed() {
            this.version.incrementAndGet();
            this.notifyDirty();
        }
    }
}
