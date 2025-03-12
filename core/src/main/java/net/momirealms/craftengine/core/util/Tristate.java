package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

public enum Tristate {
    TRUE(true),
    FALSE(false),
    UNDEFINED(false);

    public static @NotNull Tristate of(boolean val) {
        return val ? TRUE : FALSE;
    }

    public static @NotNull Tristate of(Boolean val) {
        return val == null ? UNDEFINED : val ? TRUE : FALSE;
    }

    private final boolean booleanValue;

    Tristate(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public boolean asBoolean() {
        return this.booleanValue;
    }
}
