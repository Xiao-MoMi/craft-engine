package net.momirealms.craftengine.bukkit.compatibility;

import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.compatibility.item.*;
import net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld.LegacySlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.leveler.*;
import net.momirealms.craftengine.bukkit.compatibility.model.bettermodel.BetterModelModel;
import net.momirealms.craftengine.bukkit.compatibility.model.bettermodel.BetterModelUtils;
import net.momirealms.craftengine.bukkit.compatibility.model.modelengine.ModelEngineModel;
import net.momirealms.craftengine.bukkit.compatibility.model.modelengine.ModelEngineUtils;
import net.momirealms.craftengine.bukkit.compatibility.mythicmobs.MythicItemDropListener;
import net.momirealms.craftengine.bukkit.compatibility.mythicmobs.MythicSkillHelper;
import net.momirealms.craftengine.bukkit.compatibility.papi.PlaceholderAPIUtils;
import net.momirealms.craftengine.bukkit.compatibility.permission.LuckPermsEventListeners;
import net.momirealms.craftengine.bukkit.compatibility.quickshop.QuickShopItemExpressionHandler;
import net.momirealms.craftengine.bukkit.compatibility.region.WorldGuardRegionCondition;
import net.momirealms.craftengine.bukkit.compatibility.skript.SkriptHook;
import net.momirealms.craftengine.bukkit.compatibility.slimeworld.SlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.tag.CustomNameplateProviders;
import net.momirealms.craftengine.bukkit.compatibility.viaversion.ViaVersionUtils;
import net.momirealms.craftengine.bukkit.compatibility.worldedit.WorldEditBlockRegister;
import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.loot.LootConditions;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;
import net.momirealms.craftengine.core.plugin.compatibility.ModelProvider;
import net.momirealms.craftengine.core.plugin.compatibility.TagResolverProvider;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.condition.AlwaysFalseCondition;
import net.momirealms.craftengine.core.plugin.context.event.EventConditions;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class BukkitCompatibilityManager implements CompatibilityManager {
    private final BukkitCraftEngine plugin;
    private final Map<String, ModelProvider> modelProviders;
    private final Map<String, LevelerProvider> levelerProviders;
    private final Map<String, TagResolverProvider> tagResolverProviders;
    private TagResolverProvider[] tagResolverProviderArray = null;
    private boolean hasPlaceholderAPI;

    public BukkitCompatibilityManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.modelProviders = new HashMap<>(Map.of(
                "ModelEngine", ModelEngineModel::new,
                "BetterModel", BetterModelModel::new
        ));
        this.levelerProviders = new HashMap<>();
        this.tagResolverProviders = new HashMap<>();
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
        if (this.isPluginEnabled("Skript")) {
            SkriptHook.register();
            logHook("Skript");
        }
        // WorldEdit
        // FastAsyncWorldEdit
        if (this.isPluginEnabled("FastAsyncWorldEdit")) {
            try {
                this.initFastAsyncWorldEditHook();
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
            logHook("AuraSkills");
        }
        if (this.isPluginEnabled("AureliumSkills")) {
            this.registerLevelerProvider("AureliumSkills", new AureliumSkillsLevelerProvider());
        }
        if (this.isPluginEnabled("McMMO")) {
            this.registerLevelerProvider("mcMMO", new McMMOLevelerProvider());
            logHook("McMMO");
        }
        if (this.isPluginEnabled("MMOCore")) {
            this.registerLevelerProvider("MMOCore", new MMOCoreLevelerProvider());
            logHook("MMOCore");
        }
        if (this.isPluginEnabled("Jobs")) {
            registerLevelerProvider("Jobs", new JobsRebornLevelerProvider());
            logHook("Jobs");
        }
        if (this.isPluginEnabled("EcoSkills")) {
            registerLevelerProvider("EcoSkills", new EcoSkillsLevelerProvider());
            logHook("EcoSkills");
        }
        if (this.isPluginEnabled("EcoJobs")) {
            registerLevelerProvider("EcoJobs", new EcoJobsLevelerProvider());
            logHook("EcoJobs");
        }
        if (this.isPluginEnabled("MythicMobs")) {
            BukkitItemManager.instance().registerExternalItemSource(new MythicMobsSource());
            new MythicItemDropListener(this.plugin);
            logHook("MythicMobs");
        }
        Key worldGuardRegion = Key.of("worldguard:region");
        if (this.isPluginEnabled("WorldGuard")) {
            EventConditions.register(worldGuardRegion, new WorldGuardRegionCondition.FactoryImpl<>());
            LootConditions.register(worldGuardRegion, new WorldGuardRegionCondition.FactoryImpl<>());
            logHook("WorldGuard");
        } else {
            EventConditions.register(worldGuardRegion, new AlwaysFalseCondition.FactoryImpl<>());
            LootConditions.register(worldGuardRegion, new AlwaysFalseCondition.FactoryImpl<>());
        }
        if (this.isPluginEnabled("BetterModel")) {
            BetterModelUtils.registerConstantBlockEntityRender();
            logHook("BetterModel");
        }
        if (this.isPluginEnabled("ModelEngine")) {
            ModelEngineUtils.registerConstantBlockEntityRender();
            logHook("ModelEngine");
        }
        if (this.isPluginEnabled("QuickShop-Hikari")) {
            new QuickShopItemExpressionHandler(this.plugin).register();
            logHook("QuickShop-Hikari");
        }
        if (this.isPluginEnabled("CustomNameplates")) {
            registerTagResolverProvider(new CustomNameplateProviders.Background());
            registerTagResolverProvider(new CustomNameplateProviders.Nameplate());
            registerTagResolverProvider(new CustomNameplateProviders.Bubble());
            logHook("CustomNameplates");
        }
    }

    @Override
    public void executeMMSkill(String skill, float power, Player player) {
        MythicSkillHelper.execute(skill, power, player);
    }

    @Override
    public void registerLevelerProvider(String plugin, LevelerProvider provider) {
        this.levelerProviders.put(plugin, provider);
    }

    @Override
    public void registerTagResolverProvider(TagResolverProvider provider) {
        this.tagResolverProviders.put(provider.name(), provider);
        this.tagResolverProviderArray = this.tagResolverProviders.values().toArray(new TagResolverProvider[0]);
        FormattedLine.Companion.resetWithCustomResolvers(new ArrayList<>(this.tagResolverProviders.keySet()));
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
        if (!VersionHelper.isOrAbove1_20_2()) { // 最低要求的 #1039 构建版本不支持1.20(.1)
            if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                this.plugin.logger().warn("[Compatibility] 无法兼容 FastAsyncWorldEdit，请更新到 1.20.2 或更高版本的服务端");
            } else {
                this.plugin.logger().warn("[Compatibility] Unable to hook FastAsyncWorldEdit. Please update your server to 1.20.2+");
            }
            return;
        }
        Plugin fastAsyncWorldEdit = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        String version = VersionHelper.isPaper() ? fastAsyncWorldEdit.getPluginMeta().getVersion() : fastAsyncWorldEdit.getDescription().getVersion();
        if (BlockType.REGISTRY.get("craftengine:custom_0") == null) {
            this.plugin.logger().severe("");
            if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                this.plugin.logger().severe("[Compatibility] 无法兼容 FastAsyncWorldEdit，请更新到 2.13.0 或更高版本（当前版本: " + version + "）");
                this.plugin.logger().severe("[Compatibility] 请前往 https://ci.athion.net/job/FastAsyncWorldEdit/ 下载最新版本");
            } else {
                this.plugin.logger().severe("[Compatibility] Unable to hook FastAsyncWorldEdit. Please update to v2.13.0+ (Current: " + version + ")");
                this.plugin.logger().severe("[Compatibility] Download latest version: https://ci.athion.net/job/FastAsyncWorldEdit/");
            }
            this.plugin.logger().severe("");
            return;
        }
        new WorldEditBlockRegister(BukkitBlockManager.instance(), true);
        logHook("FastAsyncWorldEdit");
    }

    private void initWorldEditHook() {
        WorldEditBlockRegister weBlockRegister = new WorldEditBlockRegister(BukkitBlockManager.instance(), false);
        try {
            for (int i = 0; i < Config.serverSideBlocks(); i++) {
                weBlockRegister.register(BlockManager.createCustomBlockKey(i));
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to initialize world edit hook", e);
        }
    }

    private void initItemHooks() {
        BukkitItemManager itemManager = BukkitItemManager.instance();
        if (this.isPluginEnabled("NeigeItems")) {
            itemManager.registerExternalItemSource(new NeigeItemsSource());
            logHook("NeigeItems");
        }
        if (this.isPluginEnabled("MMOItems")) {
            itemManager.registerExternalItemSource(new MMOItemsSource());
            logHook("MMOItems");
        }
        if (this.isPluginEnabled("CustomFishing")) {
            itemManager.registerExternalItemSource(new CustomFishingSource());
            logHook("CustomFishing");
        }
        if (this.isPluginEnabled("Zaphkiel")) {
            itemManager.registerExternalItemSource(new ZaphkielSource());
            logHook("Zaphkiel");
        }
        if (this.isPluginEnabled("HeadDatabase")) {
            itemManager.registerExternalItemSource(new HeadDatabaseSource());
            logHook("HeadDatabase");
        }
        if (this.isPluginEnabled("SX-Item")) {
            itemManager.registerExternalItemSource(new SXItemSource());
            logHook("SX-Item");
        }
        if (this.isPluginEnabled("Slimefun")) {
            itemManager.registerExternalItemSource(new SlimefunSource());
            logHook("Slimefun");
        }
        if (this.isPluginEnabled("Nexo")) {
            itemManager.registerExternalItemSource(new NexoItemSource());
            logHook("Nexo");
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

    @Override
    public TagResolver[] createExternalTagResolvers(Context context) {
        if (this.tagResolverProviderArray == null) return null;
        int length = this.tagResolverProviderArray.length;
        TagResolver[] resolvers = new TagResolver[length];
        for (int i = 0; i < length; i++) {
            resolvers[i] = this.tagResolverProviderArray[i].getTagResolver(context);
        }
        return resolvers;
    }
}
