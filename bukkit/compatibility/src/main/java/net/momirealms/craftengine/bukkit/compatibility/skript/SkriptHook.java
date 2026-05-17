package net.momirealms.craftengine.bukkit.compatibility.skript;

import ch.njol.skript.Skript;
import net.momirealms.craftengine.bukkit.compatibility.skript.clazz.CraftEngineClasses;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsCraftEngineHasBeenLoad;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsCustomBlock;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsCustomItem;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.effect.EffPlaceCustomBlock;
import net.momirealms.craftengine.bukkit.compatibility.skript.effect.EffPlaceFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.effect.EffRemoveFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCraftEngineReload;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCustomBlock;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCustomClick;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCustomFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.expression.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.addon.SkriptAddon;

import java.util.Locale;

public final class SkriptHook {
    private static SkriptAddon addon;

    private SkriptHook() {}

    public static void register() {
        if (!checkCompatibleAndWarn()) return;
        addon = Skript.instance().registerAddon(SkriptHook.class, "CraftEngine");
        CraftEngineClasses.register(addon);
        EvtCraftEngineReload.register(addon);
        EvtCustomBlock.register(addon);
        EvtCustomFurniture.register(addon);
        EvtCustomClick.register(addon);
        CondIsCraftEngineHasBeenLoad.register(addon);
        CondIsCustomBlock.register(addon);
        CondIsFurniture.register(addon);
        CondIsCustomItem.register(addon);
        ExprBlockCustomBlockID.register(addon);
        ExprItemCustomItemID.register(addon);
        ExprBlockCustomBlockState.register(addon);
        ExprCustomItem.register(addon);
        ExprEntityFurnitureID.register(addon);
        EffPlaceCustomBlock.register(addon);
        EffPlaceFurniture.register(addon);
        EffRemoveFurniture.register(addon);
    }

    public static SkriptAddon addon() {
        return addon;
    }

    @SuppressWarnings({"DataFlowIssue", "deprecation"})
    private static boolean checkCompatibleAndWarn() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Skript");
        String version = VersionHelper.isPaper ? plugin.getPluginMeta().getVersion() : plugin.getDescription().getVersion();
        version = version.split("-", 2)[0];
        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            CraftEngine.instance().logger().error("[Compatibility] Invalid Skript version: " + version);
            return false;
        }
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        boolean isSupport = major >= 2 && minor >= 15;
        if (!isSupport) {
            if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                CraftEngine.instance().logger().error("[兼容性] 插件需要更新 Skript 到 2.15.0 或更高版本。(当前版本: " + version + ")");
                CraftEngine.instance().logger().error("[兼容性] 请前往 https://modrinth.com/plugin/skript 下载最新版本");
            } else {
                CraftEngine.instance().logger().error("[Compatibility] Update Skript to v2.15.0 or newer to enable additional features (Current version: " + version + ")");
                CraftEngine.instance().logger().error("[Compatibility] Download latest version on: https://modrinth.com/plugin/skript");
            }
        }
        return isSupport;
    }
}
