package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;

// 按周期弱缓存共享节点, 无外部持有者的实例由下次取用清理.
final class WeakPeriodCache<V> {
    private final Map<Long, Entry<V>> entries = new HashMap<>();
    private final ReferenceQueue<V> released = new ReferenceQueue<>();

    // 取该周期的实例, 缓存未命中时创建. 返回值需要由调用方保活.
    @NotNull
    synchronized V get(long period, @NotNull LongFunction<? extends V> create) {
        for (Reference<?> dead; (dead = this.released.poll()) != null; ) {
            // 仅删除仍指向该弱引用的槽, 保留已经重建的实例.
            this.entries.remove(((Entry<?>) dead).period, dead);
        }
        Entry<V> cached = this.entries.get(period);
        V value = cached == null ? null : cached.get();
        if (value == null) {
            value = create.apply(period);
            this.entries.put(period, new Entry<>(period, value, this.released));
        }
        return value;
    }

    // 包含已回收但尚未从队列清理的槽.
    synchronized int size() {
        return this.entries.size();
    }

    synchronized void clear() {
        this.entries.clear();
    }

    // 弱引用携带周期, 回收后可以直接定位缓存槽.
    private static final class Entry<V> extends WeakReference<V> {
        private final long period;

        private Entry(long period, V value, ReferenceQueue<? super V> queue) {
            super(value, queue);
            this.period = period;
        }
    }
}
