package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class MythicMechanicListener implements Listener {

    public MythicMechanicListener(BukkitCraftEngine plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        if ("takeceitem".equalsIgnoreCase(event.getMechanicName())) {
            event.register(new TakeCEItemMechanic(event.getConfig()));
        }
    }
}
