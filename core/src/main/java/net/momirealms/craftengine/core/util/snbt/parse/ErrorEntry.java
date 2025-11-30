package net.momirealms.craftengine.core.util.snbt.parse;

public record ErrorEntry<S>(int cursor, SuggestionSupplier<S> suggestions, Object reason) {
}
