package net.momirealms.craftengine.core.util.snbt.parse;

public interface NamedRule<S, T> {
    Atom<T> name();

    Rule<S, T> value();
}
