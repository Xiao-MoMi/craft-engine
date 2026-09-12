package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link KeyedSignal#at} 返回的稳定句柄, 跨越分区的删除与重建转发读值和失效.
 * <p>句柄有订阅者时才建立到分区的转发. 没有订阅者时由拉取路径对齐版本.
 *
 * @param <K> 分区 key 类型
 * @param <T> 值类型
 */
sealed class PartitionHandle<K, T> extends AbstractSignal<T> permits MutablePartitionHandle {
    private final AbstractKeyedSignal<K, T, ?> owner;
    private final K key;
    private final AtomicReference<Synced<T>> synced = new AtomicReference<>(new Synced<>(null, 0L, 0L));
    private final Object attachLock = new Object();
    @Nullable private volatile AbstractSignal<T> attached;   // 当前分区, 供 attach 幂等判断与 owner 无锁快路径读取
    private boolean active;                        // 有订阅期间为真, attachLock 内读写
    @Nullable private Subscription forward;                  // 到 attached 的转发凭证, 只在有订阅期间存在, 与 attached 一起换

    PartitionHandle(AbstractKeyedSignal<K, T, ?> owner, K key) {
        this.owner = owner;
        this.key = key;
    }

    K key() {
        return this.key;
    }

    @Override
    public T get() {
        return this.owner.partition(this.key).get();
    }

    /**
     * 版本由句柄自己维护并单调递增, <strong>不能透传分区版本</strong>, 前后两个分区各有各的计数.
     * <p>读取时与当前分区对齐, 将无订阅期间发生的变化收进句柄版本.
     */
    @Override
    long version() {
        AbstractSignal<T> partition = this.attached;
        return partition == null ? this.synced.get().version() : this.sync(partition).version();
    }

    // 分区实例或版本变化时推进句柄版本. CAS 只从当前读到的记录前进, 争用失败后重读.
    private Synced<T> sync(AbstractSignal<T> partition) {
        while (true) {
            long partitionVersion = partition.version();
            Synced<T> current = this.synced.get();
            if (current.partition() == partition && current.partitionVersion() == partitionVersion) {
                return current;
            }
            Synced<T> next = new Synced<>(partition, partitionVersion, current.version() + 1L);
            if (this.synced.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    // 结果允许在并发换挂后立刻过期. false 只会多走慢路径, true 表示该挂载关系确实存在过.
    boolean isAttachedTo(AbstractSignal<T> partition) {
        return this.attached == partition;
    }

    /**
     * 跟上一个分区, 有订阅者时一并建立它到本句柄的转发, 换挂时自己关掉上一条.
     * <p><strong>只能在 owner 对应 key 的 compute 中调用</strong>, 与分区驱逐保持串行.
     */
    void attach(AbstractSignal<T> partition) {
        Subscription previous;
        synchronized (this.attachLock) {
            if (this.attached == partition) {
                return;
            }
            previous = this.forward;
            // 弱订阅让分区无法反向保活句柄
            this.forward = this.active ? this.linkTo(partition, this::onPartitionDirty) : null;
            this.sync(partition);
            // attached 是换挂完成的发布标志, 在转发建立与版本推进之后写入
            this.attached = partition;
        }
        if (previous != null) {
            previous.close();
        }
    }

    // 分区删除时摘掉转发并暂时脱离, 调用方持有对应 key 的 compute 锁
    void onPartitionEvicted(AbstractSignal<T> evicted) {
        Subscription previous;
        synchronized (this.attachLock) {
            // 迟到的旧驱逐不能摘掉已经换上的新分区
            if (this.attached != evicted) return;
            previous = this.forward;
            this.attached = null;
            this.forward = null;
            // 先推进句柄版本, 同时释放已终止的旧分区
            this.synced.updateAndGet(current -> new Synced<>(null, 0L, current.version() + 1L));
        }
        if (previous != null) {
            previous.close();
        }
    }

    @Override
    protected void onActive() {
        synchronized (this.attachLock) {
            this.active = true;
            AbstractSignal<T> partition = this.attached;
            if (partition != null) {
                // 先挂转发再对版本, 订阅前的变化并入基线且不补发
                this.forward = this.linkTo(partition, this::onPartitionDirty);
                this.sync(partition);
            }
        }
    }

    // 分区标脏时句柄也推进一次版本并转发.
    private void onPartitionDirty() {
        AbstractSignal<T> partition = this.attached;
        if (partition != null) {
            this.sync(partition);
        }
        this.notifyDirty();
    }

    @Override
    protected void onInactive() {
        Subscription previous;
        synchronized (this.attachLock) {
            this.active = false;
            previous = this.forward;
            this.forward = null;
        }
        if (previous != null) {
            previous.close();
        }
    }

    // 最近一次对齐的分区版本与句柄版本
    private record Synced<V>(@Nullable AbstractSignal<V> partition, long partitionVersion, long version) {
    }
}
