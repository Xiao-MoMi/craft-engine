package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Locale;

public final class MythicItemDropListener implements Listener {
    private final BukkitCraftEngine plugin;

    public MythicItemDropListener(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicDropLoad(MythicDropLoadEvent event)	{
        if (!"craftengine".equalsIgnoreCase(event.getDropName())) {
            return;
        }
        String line = event.getContainer().getLine().toLowerCase(Locale.ROOT);
        String itemId = extractItemId(line);
        if (itemId == null || itemId.isEmpty()) {
            return;
        }
        event.register(new MythicItemDrop(line, event.getConfig(),
                LazyReference.lazyReference(() -> this.plugin.itemManager().getCustomItem(Key.of(itemId)).orElse(null)),
                itemId
        ));
    }

    private static String extractItemId(String line) {
        if (line.startsWith("craftengine ")) {
            int nextSpaceIndex = line.indexOf(' ', 12);
            return nextSpaceIndex == -1
                    ? line.substring(12)
                    : line.substring(12, nextSpaceIndex);
        }
        if (line.startsWith("craftengine:")) {
            int spaceIndex = line.indexOf(' ', 12);
            return spaceIndex == -1
                    ? line.substring(12)
                    : line.substring(12, spaceIndex);
        }
        return null;
    }
}
