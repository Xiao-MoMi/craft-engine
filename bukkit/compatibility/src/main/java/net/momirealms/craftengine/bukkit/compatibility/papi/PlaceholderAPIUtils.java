package net.momirealms.craftengine.bukkit.compatibility.papi;

import me.clip.placeholderapi.PlaceholderAPI;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlaceholderAPIUtils {

    private PlaceholderAPIUtils() {}

    public static String parse(OfflinePlayer player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static String parse(Player player1, Player player2, String text) {
        return PlaceholderAPI.setRelationalPlaceholders(player1, player2, text);
    }

    public static void registerExpansions(BukkitCraftEngine plugin) {
        new ImageExpansion(plugin).register();
        new ShiftExpansion(plugin).register();
        new CheckItemExpansion(plugin).register();
        new CraftEngineExpansion(plugin).register();
    }
}
