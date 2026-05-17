package net.momirealms.craftengine.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public final class EventUtils {
    private EventUtils() {}

    public static void fireAndForget(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static <T extends Event & Cancellable> boolean fireAndCheckCancel(T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
