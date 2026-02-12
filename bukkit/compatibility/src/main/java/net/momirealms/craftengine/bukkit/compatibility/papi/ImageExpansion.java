package net.momirealms.craftengine.bukkit.compatibility.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ImageExpansion extends PlaceholderExpansion {
    private final BukkitCraftEngine plugin;

    public ImageExpansion(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "image";
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
        if (split.length != 2) return null;
        String[] param = split[1].split(":", 4);
        Optional<Image> optional;
        if (param.length == 1) {
            optional = this.plugin.fontManager().imageByIdValue(param[0]);
        } else {
            Key key;
            try {
                key = Key.of(param[0], param[1]);
            } catch (IllegalArgumentException e) {
                this.plugin.logger().warn("Invalid image namespaced key: " + param[0] + ":" + param[1]);
                return null;
            }
            optional = this.plugin.fontManager().imageById(key);
        }
        if (optional.isEmpty()) {
            return null;
        }
        Image image = optional.get();
        int row;
        int col;
        if (param.length == 4) {
            row = Integer.parseInt(param[2]);
            col = Integer.parseInt(param[3]);
        } else if (param.length == 3) {
            row = Integer.parseInt(param[2]);
            col = 0;
        } else {
            row = 0;
            col = 0;
        }
        try {
            switch (split[0]) {
                case "mm", "minimessage", "mini" -> {
                    return image.miniMessageAt(row, col);
                }
                case "md", "minedown" -> {
                    return image.mineDownAt(row, col);
                }
                case "raw" -> {
                    return new String(Character.toChars(image.codepointAt(row, col)));
                }
                default -> {
                    return null;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}

