package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Function;

sealed class AsyncKeyedSignalImpl<K, T> extends AbstractKeyedSignal<K, T, AsyncSignalImpl<T>> permits PlayerKeyedSignalImpl {
    private final T placeholder;
    private final Executor executor;
    private final Function<? super K, ? extends T> loader;
    private final BiPredicate<? super T, ? super T> sameValue;
    @Nullable private final AsyncSignalImpl.Polling polling;    // 全部分区共用时钟设置, 各分区独立启停

    AsyncKeyedSignalImpl(T placeholder, Executor executor, Function<? super K, ? extends T> loader, BiPredicate<? super T, ? super T> sameValue, @Nullable AsyncSignalImpl.Polling polling) {
        this.placeholder = placeholder;
        this.executor = executor;
        this.loader = loader;
        this.sameValue = sameValue;
        this.polling = polling;
    }

    @Override
    AsyncSignalImpl<T> createPartition(K key) {
        return new AsyncSignalImpl<>(this.placeholder, this.executor, () -> this.loader.apply(key), this.sameValue, this.polling);
    }

    // 真正读写分区时推动首载, 单个分区只会成功调度一次
    @Override
    void afterPartitionAccess(AsyncSignalImpl<T> partition) {
        partition.scheduleInitialLoad();
    }

    @Override
    void dirtyPartition(AsyncSignalImpl<T> partition) {
        partition.dirty();
    }
}
