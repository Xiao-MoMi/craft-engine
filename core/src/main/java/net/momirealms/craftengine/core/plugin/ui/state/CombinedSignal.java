package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

final class CombinedSignal<T> extends AbstractSignal<T> {
    private final AbstractSignal<?>[] sources;
    private final Function<Object[], ? extends T> combiner;
    @Nullable private volatile Cached<T> cached;
    private Subscription @Nullable [] upstream;

    CombinedSignal(AbstractSignal<?>[] sources, @NotNull Function<Object[], ? extends T> combiner) {
        this.sources = sources;
        this.combiner = combiner;
    }

    @Override
    public T get() {
        long versionSum = this.versionSum();
        Cached<T> current = this.cached;
        if (current != null && current.versionSum() == versionSum) {
            return current.value();
        }
        Object[] values = new Object[this.sources.length];
        for (int index = 0; index < this.sources.length; index++) {
            values[index] = this.sources[index].get();
        }
        T value = this.combiner.apply(values);
        this.cached = new Cached<>(value, versionSum);
        return value;
    }

    @Override
    long version() {
        return this.versionSum();
    }

    private long versionSum() {
        long sum = 0L;
        for (int index = 0; index < this.sources.length; index++) {
            sum += this.sources[index].version();
        }
        return sum;
    }

    @Override
    protected void onActive() {
        this.upstream = this.linkAll(this.sources, this::notifyDirty);
    }

    @Override
    protected void onInactive() {
        closeAll(this.upstream);
        this.upstream = null;
    }

    private record Cached<V>(V value, long versionSum) {
    }
}
