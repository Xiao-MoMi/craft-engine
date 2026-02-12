package net.momirealms.craftengine.bukkit.compatibility.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CraftEngineExpansion extends PlaceholderExpansion {
    private final CraftEngine plugin;

    public CraftEngineExpansion(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "ce";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "XiaoMoMi";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0";
    }

    /**
     * 用法:（小括号括起来的为必填，中括号括起来的为选填）
     * <p>
     * %ce_cd_(key)|[format]%
     */
    @Override
    public @Nullable String onPlaceholderRequest(Player bukkitPlayer, @NotNull String params) {
        BukkitServerPlayer player = bukkitPlayer != null ? BukkitAdaptors.adapt(bukkitPlayer) : null;
        String[] split = params.split("_", 2);
        if (split.length == 2) {
            return switch (split[0]) {
                case "cd", "cooldown" -> Optional.ofNullable(getCooldown(player, split[1])).orElse("0");
                default -> null;
            };
        }
        return null;
    }

    private static String getCooldown(BukkitServerPlayer player, String param) {
        if (player == null) {
            return null;
        }
        CooldownData cooldown = player.cooldown();
        if (cooldown == null) {
            return null;
        }
        Long ms = cooldown.getExpirationTime(param);
        if (ms == null) {
            return null;
        }
        ms -= System.currentTimeMillis();
        if (ms < 0) {
            return null;
        }
        return String.valueOf(ms);
    }
}
