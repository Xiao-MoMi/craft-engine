package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

final class MutablePartitionHandle<K, T> extends PartitionHandle<K, T> implements MutableSignal<T> {
    private final KeyedSignalImpl<K, T> owner;

    MutablePartitionHandle(KeyedSignalImpl<K, T> owner, K key) {
        super(owner, key);
        this.owner = owner;
    }

    @Override
    public void set(T value) {
        this.owner.set(this.key(), value);
    }

    @Override
    public void update(@NotNull UnaryOperator<T> updater) {
        this.owner.update(this.key(), updater);
    }
}
