package net.momirealms.craftengine.bukkit.entity.furniture;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.BiConsumer;

public class DismountListener1_20 implements Listener {
    private final BiConsumer<Player, Entity> consumer;

    public DismountListener1_20(BiConsumer<Player, Entity> consumer) {
        this.consumer = consumer;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDismount(org.spigotmc.event.entity.EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.consumer.accept(player, event.getDismounted());
        }
    }
}
