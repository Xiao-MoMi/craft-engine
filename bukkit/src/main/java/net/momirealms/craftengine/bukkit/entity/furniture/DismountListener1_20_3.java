package net.momirealms.craftengine.bukkit.entity.furniture;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

public class DismountListener1_20_3 implements Listener {
    private final BukkitFurnitureManager manager;

    public DismountListener1_20_3(final BukkitFurnitureManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.manager.handleDismount(player, event.getDismounted());
        }
    }
}
