package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftEngineReloadEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private static boolean hasBeenTriggeredBefore = false;
    private final BukkitCraftEngine plugin;
    private final boolean isFirstLoad;

    public CraftEngineReloadEvent(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.isFirstLoad = !hasBeenTriggeredBefore;
        if (!hasBeenTriggeredBefore) hasBeenTriggeredBefore = true;
    }

    public BukkitCraftEngine plugin() {
        return plugin;
    }

    public boolean isFirstLoad() {
        return isFirstLoad;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
