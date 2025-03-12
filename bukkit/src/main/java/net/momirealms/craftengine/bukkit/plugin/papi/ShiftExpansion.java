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
                    return plugin.imageManager().createMiniMessageOffsets(Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case "md", "minedown" -> {
                if (split.length != 2) return null;
                try {
                    return plugin.imageManager().createMineDownOffsets(Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case "raw" -> {
                if (split.length != 2) return null;
                try {
                    return plugin.imageManager().createRawOffsets(Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            default -> {
                if (split.length != 1) return null;
                try {
                    return plugin.imageManager().createMiniMessageOffsets(Integer.parseInt(split[0]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
    }
}

