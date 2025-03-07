package net.momirealms.craftengine.bukkit.plugin.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShiftExpansion extends PlaceholderExpansion {
    private final CraftEngine plugin;

    public ShiftExpansion(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "shift";
    }

    @Override
    public @NotNull String getAuthor() {
        return "XiaoMoMi";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] split = params.split("_", 2);
        switch (split[0]) {
            case "mini", "minimessage", "mm" -> {
                if (split.length != 2) return null;
                try {
                    return plugin.fontManager().createMiniMessageOffsets(Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case "md", "minedown" -> {
                if (split.length != 2) return null;
                try {
                    return plugin.fontManager().createMineDownOffsets(Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case "raw" -> {
                if (split.length != 2) return null;
                try {
                    return plugin.fontManager().createRawOffsets(Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            default -> {
                if (split.length != 1) return null;
                try {
                    return plugin.fontManager().createMiniMessageOffsets(Integer.parseInt(split[0]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
    }
}

