package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MythicItemDropListener implements Listener {
    private final BukkitCraftEngine plugin;

    public MythicItemDropListener(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onMythicDropLoad(MythicDropLoadEvent event)	{
        if (!event.getDropName().equalsIgnoreCase("craftengine")) return;
        String argument = event.getArgument();
        Key itemId = Key.of(argument);
        this.plugin.itemManager().getCustomItem(itemId).ifPresent(customItem -> {
            String line = event.getContainer().getConfigLine();
            MythicLineConfig config = event.getConfig();
            event.register(new MythicItemDrop(line, config, customItem));
        });
    }
}
