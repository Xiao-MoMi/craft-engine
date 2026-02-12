package net.momirealms.craftengine.core.plugin.network;

public record IllegalCharacterProcessResult(boolean has, String text) {

    public static IllegalCharacterProcessResult has(String text) {
        return new IllegalCharacterProcessResult(true, text);
    }

    public static IllegalCharacterProcessResult not() {
        return new IllegalCharacterProcessResult(false, "");
    }
}