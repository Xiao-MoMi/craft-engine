package net.momirealms.craftengine.bukkit.compatibility;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.compatibility.bettermodel.BetterModelModel;
import net.momirealms.craftengine.bukkit.compatibility.item.MMOItemsProvider;
import net.momirealms.craftengine.bukkit.compatibility.item.NeigeItemsProvider;
import net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld.LegacySlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.modelengine.ModelEngineModel;
import net.momirealms.craftengine.bukkit.compatibility.modelengine.ModelEngineUtils;
import net.momirealms.craftengine.bukkit.compatibility.papi.PlaceholderAPIUtils;
import net.momirealms.craftengine.bukkit.compatibility.permission.LuckPermsEventListeners;
import net.momirealms.craftengine.bukkit.compatibility.skript.SkriptHook;
import net.momirealms.craftengine.bukkit.compatibility.slimeworld.SlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.viaversion.ViaVersionUtils;
import net.momirealms.craftengine.bukkit.compatibility.worldedit.WorldEditBlockRegister;
import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.furniture.AbstractExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CompatibilityManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class BukkitCompatibilityManager implements CompatibilityManager {
    private final BukkitCraftEngine plugin;
    private boolean hasPlaceholderAPI;
    private boolean hasViaVersion;

    public BukkitCompatibilityManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        this.initSlimeWorldHook();
        if (this.isPluginEnabled("PlaceholderAPI")) {
            PlaceholderAPIUtils.registerExpansions(this.plugin);
            this.hasPlaceholderAPI = true;
            logHook("PlaceholderAPI");
        }
        // skript
        if (this.isPluginEnabled("Skript")) {
            SkriptHook.register();
            logHook("Skript");
            Plugin skriptPlugin = getPlugin("Skript");
            // This can cause bugs, needs to find a better way
//            for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
//                if (task.getOwner() == skriptPlugin) {
//                    task.cancel();
//                    if (VersionHelper.isFolia()) {
//                        Bukkit.getGlobalRegionScheduler().run(skriptPlugin, (t) -> {
//                            FastNMS.INSTANCE.getBukkitTaskRunnable(task).run();
//                        });
//                    } else {
//                        Bukkit.getScheduler().runTask(skriptPlugin, FastNMS.INSTANCE.getBukkitTaskRunnable(task));
//                    }
//                }
//            }
        }
    }

    @Override
    public void onDelayedEnable() {
        this.initItemHooks();
        // WorldEdit
        if (this.isPluginEnabled("FastAsyncWorldEdit")) {
            this.initFastAsyncWorldEditHook();
            logHook("FastAsyncWorldEdit");
        } else if (this.isPluginEnabled("WorldEdit")) {
            this.initWorldEditHook();
            logHook("WorldEdit");
        }
        if (this.isPluginEnabled("LuckPerms")) {
            this.initLuckPermsHook();
            logHook("LuckPerms");
        }
    }

    private void logHook(String plugin) {
        this.plugin.logger().info("[Compatibility] " + plugin + " hooked");
    }

    @Override
    public AbstractExternalModel createModelEngineModel(String id) {
        return new ModelEngineModel(id);
    }

    @Override
    public AbstractExternalModel createBetterModelModel(String id) {
        return new BetterModelModel(id);
    }

    @Override
    public int interactionToBaseEntity(int id) {
        return ModelEngineUtils.interactionToBaseEntity(id);
    }

    private void initLuckPermsHook() {
        new LuckPermsEventListeners(plugin.bootstrap(), (uuid) -> {
            BukkitFontManager fontManager = (BukkitFontManager) plugin.fontManager();
            fontManager.refreshEmojiSuggestions(uuid);
        });
    }

    private void initSlimeWorldHook() {
        WorldManager worldManager = this.plugin.worldManager();
        if (VersionHelper.isOrAbove1_21_4()) {
            try {
                Class.forName("com.infernalsuite.asp.api.AdvancedSlimePaperAPI");
                SlimeFormatStorageAdaptor adaptor = new SlimeFormatStorageAdaptor(worldManager);
                worldManager.setStorageAdaptor(adaptor);
                Bukkit.getPluginManager().registerEvents(adaptor, plugin.bootstrap());
                logHook("AdvancedSlimePaper");
            } catch (ClassNotFoundException ignored) {
            }
        } else {
            try {
                Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
                LegacySlimeFormatStorageAdaptor adaptor = new LegacySlimeFormatStorageAdaptor(worldManager, 1);
                worldManager.setStorageAdaptor(adaptor);
                Bukkit.getPluginManager().registerEvents(adaptor, plugin.bootstrap());
                logHook("AdvancedSlimePaper");
            } catch (ClassNotFoundException ignored) {
                if (Bukkit.getPluginManager().isPluginEnabled("SlimeWorldPlugin")) {
                    LegacySlimeFormatStorageAdaptor adaptor = new LegacySlimeFormatStorageAdaptor(worldManager, 2);
                    worldManager.setStorageAdaptor(adaptor);
                    Bukkit.getPluginManager().registerEvents(adaptor, plugin.bootstrap());
                    logHook("AdvancedSlimePaper");
                }
            }
        }
    }

    private void initFastAsyncWorldEditHook() {
        new WorldEditBlockRegister(BukkitBlockManager.instance(), true);
    }

    private void initWorldEditHook() {
        WorldEditBlockRegister weBlockRegister = new WorldEditBlockRegister(BukkitBlockManager.instance(), false);
        try {
            for (Key newBlockId : BukkitBlockManager.instance().blockRegisterOrder()) {
                weBlockRegister.register(newBlockId);
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to initialize world edit hook", e);
        }
    }

    private void initItemHooks() {
        BukkitItemManager itemManager = BukkitItemManager.instance();
        if (this.isPluginEnabled("NeigeItems")) {
            itemManager.registerExternalItemProvider(new NeigeItemsProvider());
            logHook("NeigeItems");
        }
        if (this.isPluginEnabled("MMOItems")) {
            itemManager.registerExternalItemProvider(new MMOItemsProvider());
            logHook("MMOItems");
        }
    }

    private Plugin getPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }

    @Override
    public boolean hasPlaceholderAPI() {
        return this.hasPlaceholderAPI;
    }

    @Override
    public boolean isPluginEnabled(String plugin) {
        return Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    @Override
    public boolean hasPlugin(String plugin) {
        return Bukkit.getPluginManager().getPlugin(plugin) != null;
    }

    @Override
    public String parse(Player player, String text) {
        return PlaceholderAPIUtils.parse((org.bukkit.entity.Player) player.platformPlayer(), text);
    }

    @Override
    public String parse(Player player1, Player player2, String text) {
        return PlaceholderAPIUtils.parse((org.bukkit.entity.Player) player1.platformPlayer(), (org.bukkit.entity.Player) player2.platformPlayer(), text);
    }

    @Override
    public int getPlayerProtocolVersion(UUID uuid) {
        return ViaVersionUtils.getPlayerProtocolVersion(uuid);
    }
}
