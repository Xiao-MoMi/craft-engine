package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.UnaryOperator;

sealed class KeyedSignalImpl<K, T> extends AbstractKeyedSignal<K, T, KeyedSignalImpl.SyncPartition<K, T>> implements MutableKeyedSignal<K, T> permits MutablePlayerKeyedSignalImpl {
    private final Function<? super K, ? extends T> initial;
    private final BiPredicate<? super T, ? super T> sameValue;

    KeyedSignalImpl(Function<? super K, ? extends T> initial) {
        this(initial, AbstractSignal.defaultSameValue());
    }

    KeyedSignalImpl(Function<? super K, ? extends T> initial, BiPredicate<? super T, ? super T> sameValue) {
        this.initial = initial;
        this.sameValue = sameValue;
    }

    @Override
    SyncPartition<K, T> createPartition(K key) {
        return new SyncPartition<>(key, this.initial, this.sameValue);
    }

    @Override
    MutablePartitionHandle<K, T> createHandle(K key) {
        return new MutablePartitionHandle<>(this, key);
    }

    @Override
    @NotNull
    public MutablePartitionHandle<K, T> at(@NotNull K key) {
        return (MutablePartitionHandle<K, T>) super.at(key);
    }

    @Override
    void dirtyPartition(SyncPartition<K, T> partition) {
        partition.dirty();
    }

    @Override
    public void set(@NotNull K key, T value) {
        this.partition(key).set(value);
    }

    @Override
    public void update(@NotNull K key, @NotNull UnaryOperator<T> updater) {
        this.partition(key).update(updater);
    }

    // 同步分区在首次读取或标脏后的下一次读取中装载
    static final class SyncPartition<K, T> extends AbstractSignal<T> {
        // 连续并发失效时限制单次 get 的装载次数, 触顶后返回最后一次结果并保持 stale
        private static final int MAX_LOAD_ATTEMPTS = 8;

        private final K key;
        private final Function<? super K, ? extends T> initial;
        private final BiPredicate<? super T, ? super T> sameValue;
        private final AtomicReference<PartitionState<T>> state = new AtomicReference<>(new PartitionState<>(null, 0L, true));

        private SyncPartition(K key, Function<? super K, ? extends T> initial, BiPredicate<? super T, ? super T> sameValue) {
            this.key = key;
            this.initial = initial;
            this.sameValue = sameValue;
        }

        @Override
        public T get() {
            T value = null;
            // CAS 失败表示装载期间状态又变了, 重读后在上限内继续装载
            for (int attempt = 0; attempt < MAX_LOAD_ATTEMPTS; attempt++) {
                PartitionState<T> current = this.state.get();
                if (!current.stale()) {
                    return current.value();
                }
                value = this.initial.apply(this.key);
                if (this.state.compareAndSet(current, new PartitionState<>(value, current.version(), false))) {
                    return value;
                }
            }
            return value;
        }

        @Override
        long version() {
            return this.state.get().version();
        }

        void set(T value) {
            // 每一轮总有某个线程成功, 系统整体一定前进
            while (true) {
                PartitionState<T> current = this.state.get();
                if (!current.stale() && same(this.sameValue, current.value(), value)) {
                    return;
                }
                if (this.state.compareAndSet(current, new PartitionState<>(value, current.version() + 1, false))) {
                    this.notifyDirty();
                    return;
                }
            }
        }

        void update(UnaryOperator<T> updater) {
            // 每一轮总有某个线程成功, 系统整体一定前进
            while (true) {
                PartitionState<T> current = this.state.get();
                T base = current.stale() ? this.initial.apply(this.key) : current.value();
                T value = updater.apply(base);
                if (!current.stale() && same(this.sameValue, current.value(), value)) {
                    return;
                }
                if (this.state.compareAndSet(current, new PartitionState<>(value, current.version() + 1, false))) {
                    this.notifyDirty();
                    return;
                }
            }
        }

        // 重复标脏仍推进版本, 每次通知都能使下游缓存过期
        void dirty() {
            while (true) {
                PartitionState<T> current = this.state.get();
                if (this.state.compareAndSet(current, new PartitionState<>(current.value(), current.version() + 1, true))) {
                    this.notifyDirty();
                    return;
                }
            }
        }

        private record PartitionState<V>(V value, long version, boolean stale) {
        }
    }
}
