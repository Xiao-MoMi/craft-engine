package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftEngineReloadEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final BukkitCraftEngine plugin;

    public CraftEngineReloadEvent(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    public BukkitCraftEngine plugin() {
        return plugin;
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
