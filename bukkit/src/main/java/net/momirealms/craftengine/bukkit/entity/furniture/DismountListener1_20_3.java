package net.momirealms.craftengine.bukkit.entity.furniture;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

import java.util.function.BiConsumer;

public final class DismountListener1_20_3 implements Listener {
    private final BiConsumer<Player, Entity> consumer;

    public DismountListener1_20_3(final BiConsumer<Player, Entity> consumer) {
        this.consumer = consumer;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.consumer.accept(player, event.getDismounted());
        }
    }
}
