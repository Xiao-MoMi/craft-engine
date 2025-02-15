package net.momirealms.craftengine.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class EventUtils {

    private EventUtils() {}

    public static void fireAndForget(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static boolean fireAndCheckCancel(Event event) {
        if (!(event instanceof Cancellable cancellable))
            throw new IllegalArgumentException("Only cancellable events are allowed here");
        Bukkit.getPluginManager().callEvent(event);
        return cancellable.isCancelled();
    }
}
