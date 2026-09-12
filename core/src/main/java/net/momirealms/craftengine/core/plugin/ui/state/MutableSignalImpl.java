package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

final class MutableSignalImpl<T> extends AbstractSignal<T> implements MutableSignal<T> {
    private final BiPredicate<? super T, ? super T> sameValue;
    private final AtomicReference<Versioned<T>> state;

    MutableSignalImpl(T initial) {
        this(initial, defaultSameValue());
    }

    MutableSignalImpl(T initial, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        this.sameValue = sameValue;
        this.state = new AtomicReference<>(new Versioned<>(initial, 0L));
    }

    @Override
    public T get() {
        return this.state.get().value();
    }

    @Override
    long version() {
        return this.state.get().version();
    }

    @Override
    public void set(T value) {
        while (true) {
            Versioned<T> current = this.state.get();
            if (same(this.sameValue, current.value(), value)) {
                return;
            }
            if (this.state.compareAndSet(current, new Versioned<>(value, current.version() + 1))) {
                this.notifyDirty();
                return;
            }
        }
    }

    @Override
    public void update(@NotNull UnaryOperator<T> updater) {
        while (true) {
            Versioned<T> current = this.state.get();
            T value = updater.apply(current.value());
            if (same(this.sameValue, current.value(), value)) {
                return;
            }
            if (this.state.compareAndSet(current, new Versioned<>(value, current.version() + 1))) {
                this.notifyDirty();
                return;
            }
        }
    }
}
