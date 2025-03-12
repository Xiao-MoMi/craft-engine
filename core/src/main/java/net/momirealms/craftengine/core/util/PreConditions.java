package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

public class PreConditions {

    private PreConditions() {}

    public static boolean runIfTrue(boolean value, @NotNull Runnable runnable) {
        if (value) {
            runnable.run();
        }
        return value;
    }

    public static boolean isNull(Object value, @NotNull Runnable runnable) {
        if (value == null) {
            runnable.run();
        }
        return value == null;
    }
}
