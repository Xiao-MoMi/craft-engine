package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.UnaryOperator;

final class LensSignal<S, F> extends MapDistinctSignal<S, F> implements MutableSignal<F> {
    private final MutableSignal<S> host;
    private final Function<? super S, ? extends F> getter;
    private final BiFunction<? super S, ? super F, ? extends S> setter;

    LensSignal(
            @NotNull MutableSignal<S> host,
            @NotNull Function<? super S, ? extends F> getter,
            @NotNull BiFunction<? super S, ? super F, ? extends S> setter,
            @NotNull BiPredicate<? super F, ? super F> sameValue
    ) {
        super(require(host), getter, sameValue);
        this.host = host;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void set(F value) {
        this.host.update(current -> this.setter.apply(current, value));
    }

    @Override
    public void update(@NotNull UnaryOperator<F> updater) {
        this.host.update(current -> {
            F field = this.getter.apply(current);
            return this.setter.apply(current, updater.apply(field));
        });
    }
}
