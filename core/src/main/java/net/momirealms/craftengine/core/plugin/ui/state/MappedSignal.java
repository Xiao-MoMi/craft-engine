package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

final class MappedSignal<S, T> extends AbstractSignal<T> {
    private final AbstractSignal<S> source;
    private final Function<? super S, ? extends T> mapper;
    @Nullable private volatile Cached<T> cached;
    @Nullable private Subscription upstream;

    MappedSignal(@NotNull AbstractSignal<S> source, @NotNull Function<? super S, ? extends T> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public T get() {
        long sourceVersion = this.source.version();
        Cached<T> current = this.cached;
        if (current != null && current.sourceVersion() == sourceVersion) {
            return current.value();
        }
        T value = this.mapper.apply(this.source.get());
        this.cached = new Cached<>(value, sourceVersion);
        return value;
    }

    @Override
    long version() {
        return this.source.version();
    }

    @Override
    protected void onActive() {
        this.upstream = this.linkTo(this.source, this::notifyDirty);
    }

    @Override
    protected void onInactive() {
        this.upstream.close();
        this.upstream = null;
    }

    private record Cached<V>(V value, long sourceVersion) {
    }
}
