package net.momirealms.craftengine.core.util.snbt.parse;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public record Atom<T>(String name) {
    @Override
    public @NotNull String toString() {
        return "<" + this.name + ">";
    }

    public static <T> Atom<T> of(String name) {
        return new Atom<>(name);
    }
}
