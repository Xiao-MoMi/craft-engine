/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.bukkit.plugin.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FormatUtils;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ImageExpansion extends PlaceholderExpansion {
    private final CraftEngine plugin;

    public ImageExpansion(CraftEngine plugin) {
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
        if (param.length < 2) return null;
        Key key;
        try {
            key = Key.of(param[0], param[1]);
        } catch (IllegalArgumentException e) {
            plugin.logger().warn("Invalid image namespaced key: " + param[0] + ":" + param[1]);
            return null;
        }
        Optional<BitmapImage> optional = plugin.imageManager().bitmapImageByImageId(key);
        if (optional.isEmpty()) {
            return null;
        }
        BitmapImage image = optional.get();
        int codepoint;
        if (param.length == 4) {
            codepoint = image.codepointAt(Integer.parseInt(param[2]), Integer.parseInt(param[3]));
        } else if (param.length == 2) {
            codepoint = image.codepointAt(0,0);
        } else {
            return null;
        }
        try {
            switch (split[0]) {
                case "mm", "minimessage", "mini" -> {
                    return FormatUtils.miniMessageFont(new String(Character.toChars(codepoint)), image.font().toString());
                }
                case "md", "minedown" -> {
                    return FormatUtils.mineDownFont(new String(Character.toChars(codepoint)), image.font().toString());
                }
                case "raw" -> {
                    return new String(Character.toChars(codepoint));
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

