package net.momirealms.craftengine.core.util;

public final class ByteBuddyUtils {

    private ByteBuddyUtils() {
    }

    public static void configureUnsafeInjection() {
        if (Runtime.version().feature() >= 26) {
            System.getProperties().putIfAbsent("net.momirealms.craftengine.libraries.bytebuddy.safe", "false");
        }
    }
}
