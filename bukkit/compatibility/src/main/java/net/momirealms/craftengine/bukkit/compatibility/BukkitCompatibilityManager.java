package net.momirealms.craftengine.bukkit.compatibility;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.compatibility.bettermodel.BetterModelModel;
import net.momirealms.craftengine.bukkit.compatibility.item.MMOItemsProvider;
import net.momirealms.craftengine.bukkit.compatibility.item.NeigeItemsProvider;
import net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld.LegacySlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.leveler.AuraSkillsLevelerProvider;
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
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;
import net.momirealms.craftengine.core.plugin.compatibility.ModelProvider;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BukkitCompatibilityManager implements CompatibilityManager {
    private final BukkitCraftEngine plugin;
    private final Map<String, ModelProvider> modelProviders;
    private final Map<String, LevelerProvider> levelerProviders;
    private boolean hasPlaceholderAPI;
    private boolean hasViaVersion;

    public BukkitCompatibilityManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.modelProviders = new HashMap<>(Map.of(
                "ModelEngine", ModelEngineModel::new,
                "BetterModel", BetterModelModel::new
        ));
        this.levelerProviders = new HashMap<>();
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
        // WorldEdit
        if (this.isPluginEnabled("FastAsyncWorldEdit")) {
            try {
                this.initFastAsyncWorldEditHook();
                logHook("FastAsyncWorldEdit");
            } catch (Exception e) {
                this.plugin.logger().warn("[Compatibility] Failed to initialize FastAsyncWorldEdit hook", e);
            }
        } else if (this.isPluginEnabled("WorldEdit")) {
            this.initWorldEditHook();
            logHook("WorldEdit");
        }
    }

    @Override
    public void onDelayedEnable() {
        this.initItemHooks();

        if (this.isPluginEnabled("LuckPerms")) {
            this.initLuckPermsHook();
            logHook("LuckPerms");
        }
        if (this.isPluginEnabled("AuraSkills")) {
            this.registerLevelerProvider("AuraSkills", new AuraSkillsLevelerProvider());
        }
    }

    @Override
    public void registerLevelerProvider(String plugin, LevelerProvider provider) {
        this.levelerProviders.put(plugin, provider);
    }

    private void logHook(String plugin) {
        this.plugin.logger().info("[Compatibility] " + plugin + " hooked");
    }

    @Override
    public void addLevelerExp(Player player, String plugin, String target, double value) {
        Optional.ofNullable(this.levelerProviders.get(plugin)).ifPresentOrElse(leveler -> leveler.addExp(player, target, value),
                () -> this.plugin.logger().warn("[Compatibility] '" + plugin + "' leveler provider not found"));
    }

    @Override
    public int getLevel(Player player, String plugin, String target) {
        return Optional.ofNullable(this.levelerProviders.get(plugin)).map(leveler -> leveler.getLevel(player, target)).orElseGet(() -> {
            this.plugin.logger().warn("[Compatibility] '" + plugin + "' leveler provider not found");
            return 0;
        });
    }

    @Override
    public ExternalModel createModel(String plugin, String id) {
        return this.modelProviders.get(plugin).createModel(id);
    }

    @Override
    public int interactionToBaseEntity(int id) {
        return ModelEngineUtils.interactionToBaseEntity(id);
    }

    private void initLuckPermsHook() {
        new LuckPermsEventListeners(plugin.javaPlugin(), (uuid) -> {
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
                Bukkit.getPluginManager().registerEvents(adaptor, plugin.javaPlugin());
                logHook("AdvancedSlimePaper");
            } catch (ClassNotFoundException ignored) {
            }
        } else {
            try {
                Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
                LegacySlimeFormatStorageAdaptor adaptor = new LegacySlimeFormatStorageAdaptor(worldManager, 1);
                worldManager.setStorageAdaptor(adaptor);
                Bukkit.getPluginManager().registerEvents(adaptor, plugin.javaPlugin());
                logHook("AdvancedSlimePaper");
            } catch (ClassNotFoundException ignored) {
                if (Bukkit.getPluginManager().isPluginEnabled("SlimeWorldPlugin")) {
                    LegacySlimeFormatStorageAdaptor adaptor = new LegacySlimeFormatStorageAdaptor(worldManager, 2);
                    worldManager.setStorageAdaptor(adaptor);
                    Bukkit.getPluginManager().registerEvents(adaptor, plugin.javaPlugin());
                    logHook("AdvancedSlimePaper");
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "all"})
    private void initFastAsyncWorldEditHook() {
        Plugin fastAsyncWorldEdit = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        String version = VersionHelper.isPaper() ? fastAsyncWorldEdit.getPluginMeta().getVersion() : fastAsyncWorldEdit.getDescription().getVersion();
        if (!this.fastAsyncWorldEditVersionCheck(version)) {
            this.plugin.logger().warn("[Compatibility] Please update FastAsyncWorldEdit to 2.13.0 or newer for better compatibility");
        }
        new WorldEditBlockRegister(BukkitBlockManager.instance(), true);
    }

    private boolean fastAsyncWorldEditVersionCheck(String version) {
        String cleanVersion = version.split("-")[0];
        String[] parts = cleanVersion.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first >= 2 && second >= 13;
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
