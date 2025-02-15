package net.momirealms.craftengine.core.util;

@FunctionalInterface
public interface HeptaFunction<T, U, V, W, X, Y, O, R> {
    R apply(T t, U u, V v, W w, X x, Y y, O o);
}