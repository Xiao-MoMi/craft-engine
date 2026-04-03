package net.momirealms.craftengine.bukkit.compatibility;

import cn.gtemc.itembridge.api.Provider;
import cn.gtemc.itembridge.core.BukkitItemBridge;
import cn.gtemc.levelerbridge.core.BukkitLevelerBridge;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.bukkit.block.entity.renderer.constant.BukkitBlockEntityElementConfigs;
import net.momirealms.craftengine.bukkit.compatibility.bedrock.FloodgateUtils;
import net.momirealms.craftengine.bukkit.compatibility.bedrock.GeyserUtils;
import net.momirealms.craftengine.bukkit.compatibility.entity.MythicMobsEntityProvider;
import net.momirealms.craftengine.bukkit.compatibility.item.ItemBridgeSource;
import net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld.LegacySlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.leveler.LevelerBridgeLeveler;
import net.momirealms.craftengine.bukkit.compatibility.model.bettermodel.BetterModelBlockEntityElementConfig;
import net.momirealms.craftengine.bukkit.compatibility.model.bettermodel.BetterModelFurnitureElementConfig;
import net.momirealms.craftengine.bukkit.compatibility.model.bettermodel.BetterModelProvider;
import net.momirealms.craftengine.bukkit.compatibility.model.modelengine.ModelEngineBlockEntityElementConfig;
import net.momirealms.craftengine.bukkit.compatibility.model.modelengine.ModelEngineFurnitureElementConfig;
import net.momirealms.craftengine.bukkit.compatibility.model.modelengine.ModelEngineProvider;
import net.momirealms.craftengine.bukkit.compatibility.model.modelengine.ModelEngineUtils;
import net.momirealms.craftengine.bukkit.compatibility.mythicmobs.MythicItemDropListener;
import net.momirealms.craftengine.bukkit.compatibility.mythicmobs.MythicMobsSkillFunction;
import net.momirealms.craftengine.bukkit.compatibility.mythicmobs.MythicMobsSpawnFunction;
import net.momirealms.craftengine.bukkit.compatibility.nameplates.CustomNameplateHatSettings;
import net.momirealms.craftengine.bukkit.compatibility.nameplates.CustomNameplateProviders;
import net.momirealms.craftengine.bukkit.compatibility.packetevents.WrappedBlockStateHelper;
import net.momirealms.craftengine.bukkit.compatibility.papi.PlaceholderAPIUtils;
import net.momirealms.craftengine.bukkit.compatibility.permission.LuckPermsEventListeners;
import net.momirealms.craftengine.bukkit.compatibility.permission.LuckPermsUtils;
import net.momirealms.craftengine.bukkit.compatibility.quickshop.QuickShopItemExpressionHandler;
import net.momirealms.craftengine.bukkit.compatibility.skript.SkriptHook;
import net.momirealms.craftengine.bukkit.compatibility.slimeworld.SlimeFormatStorageAdaptor;
import net.momirealms.craftengine.bukkit.compatibility.viaversion.ViaVersionUtils;
import net.momirealms.craftengine.bukkit.compatibility.worldedit.WorldEditBlockRegister;
import net.momirealms.craftengine.bukkit.compatibility.worldguard.WorldGuardRegionCondition;
import net.momirealms.craftengine.bukkit.entity.furniture.element.BukkitFurnitureElementConfigs;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.*;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.condition.AlwaysFalseCondition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

@SuppressWarnings("unused")
public final class BukkitCompatibilityManager implements CompatibilityManager {
    private final BukkitCraftEngine plugin;
    private final Map<String, ModelProvider> modelProviders;
    private final Map<String, TagResolverProvider> tagResolverProviders;
    private final Map<String, ItemSource> itemSources;
    private final Map<String, LevelerProvider> levelerProviders;
    private final Map<String, EntityProvider> entityProviders;
    private TagResolverProvider[] tagResolverProviderArray = null;
    private boolean hasPlaceholderAPI;
    private boolean hasGeyser;
    private boolean hasFloodgate;
    private boolean hasLuckPerms;

    public BukkitCompatibilityManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.itemSources = new HashMap<>();
        this.levelerProviders = new HashMap<>();
        this.entityProviders = new HashMap<>();
        this.modelProviders = new HashMap<>();
        this.tagResolverProviders = new HashMap<>();
    }

    @Override
    public ModelProvider getModelProvider(final String id) {
        return this.modelProviders.get(id);
    }

    @Override
    public void registerModelProvider(final ModelProvider provider) {
        this.modelProviders.put(provider.plugin(), provider);
    }

    @Override
    public ItemSource getItemSource(String id) {
        return this.itemSources.get(id);
    }

    @Override
    public void registerItemSource(ItemSource itemSource) {
        this.itemSources.put(itemSource.plugin(), itemSource);
    }

    @Override
    public LevelerProvider getLevelerProvider(String id) {
        return this.levelerProviders.get(id);
    }

    @Override
    public void registerLevelerProvider(LevelerProvider provider) {
        this.levelerProviders.put(provider.plugin(), provider);
    }

    @Override
    public EntityProvider getEntityProvider(String id) {
        return this.entityProviders.get(id);
    }

    @Override
    public void registerEntityProvider(EntityProvider provider) {
        this.entityProviders.put(provider.plugin(), provider);
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        this.initSlimeWorldHook();
        // WorldEdit
        // FastAsyncWorldEdit
        if (this.isPluginEnabled("FastAsyncWorldEdit")) {
            runCatchingHook(this::initFastAsyncWorldEditHook, "FastAsyncWorldEdit");
        } else if (this.isPluginEnabled("WorldEdit")) {
            runCatchingHook(this::initWorldEditHook, "WorldEdit");
        }
        if (this.hasPlugin("BetterModel")) {
            runCatchingHook(() -> {
                BukkitBlockEntityElementConfigs.register(Key.ce("better_model"), new BetterModelBlockEntityElementConfig.Factory());
                BukkitFurnitureElementConfigs.register(Key.ce("better_model"), BetterModelFurnitureElementConfig.FACTORY);
                registerModelProvider(new BetterModelProvider());
            }, "BetterModel");
        }
        if (this.hasPlugin("ModelEngine")) {
            runCatchingHook(() -> {
                BukkitBlockEntityElementConfigs.register(Key.ce("model_engine"), new ModelEngineBlockEntityElementConfig.Factory());
                BukkitFurnitureElementConfigs.register(Key.ce("model_engine"), ModelEngineFurnitureElementConfig.FACTORY);
                registerModelProvider(new ModelEngineProvider());
            }, "ModelEngine");
        }
        if (this.hasPlugin("CustomNameplates")) {
            runCatchingHook(() -> {
                registerTagResolverProvider(new CustomNameplateProviders.Background());
                registerTagResolverProvider(new CustomNameplateProviders.Nameplate());
                registerTagResolverProvider(new CustomNameplateProviders.Bubble());
                new CustomNameplateHatSettings().register();
            }, "CustomNameplates");
        }
        Key worldGuardRegion = Key.of("worldguard:region");
        if (this.hasPlugin("WorldGuard")) {
            runCatchingHook(() -> CommonConditions.register(worldGuardRegion, WorldGuardRegionCondition.factory()), "WorldGuard");
        } else {
            CommonConditions.register(worldGuardRegion, AlwaysFalseCondition.factory());
        }
        if (this.hasPlugin("Geyser-Spigot")) {
            this.hasGeyser = true;
        }
        if (this.hasPlugin("floodgate")) {
            this.hasFloodgate = true;
        }
        // 对安装了 FreedomChat 的用户告警
        if (Bukkit.getPluginManager().getPlugin("FreedomChat") != null) {
            this.plugin.logger().error("");
            if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                this.plugin.logger().error("CraftEngine 与 FreedomChat 不兼容，请立即卸载 FreedomChat");
                this.plugin.logger().error("作为替代方案，请在 config.yml 中启用 disable-chat-report 选项");
            } else {
                this.plugin.logger().error("CraftEngine is incompatible with FreedomChat. Please uninstall FreedomChat immediately.");
                this.plugin.logger().error("As an alternative, enable disable-chat-report in config.yml.");
            }
            this.plugin.logger().error("");
        }
    }

    @Override
    public void onDelayedEnable() {
        if (this.isPluginEnabled("PlaceholderAPI")) {
            runCatchingHook(() -> {
                PlaceholderAPIUtils.registerExpansions(this.plugin);
                this.hasPlaceholderAPI = true;
            }, "PlaceholderAPI");
        }
        if (this.isPluginEnabled("LuckPerms")) {
            runCatchingHook(this::initLuckPermsHook, "LuckPerms");
        }
        if (this.isPluginEnabled("Skript")) {
            runCatchingHook(SkriptHook::register, "Skript");
        }
        if (this.isPluginEnabled("MythicMobs")) {
            runCatchingHook(() -> {
                new MythicItemDropListener(this.plugin);
                this.registerEntityProvider(new MythicMobsEntityProvider());
                CommonFunctions.register(Key.ce("mythic_mobs_skill"), MythicMobsSkillFunction.factory(CommonConditions::fromConfig));
                CommonFunctions.register(Key.ce("cast_mythic_skill"), MythicMobsSkillFunction.factory(CommonConditions::fromConfig));
                CommonFunctions.register(Key.ce("spawn_mythic_mob"), MythicMobsSpawnFunction.factory(CommonConditions::fromConfig));
            }, "MythicMobs");
        }
        if (this.isPluginEnabled("QuickShop-Hikari")) {
            runCatchingHook(() -> new QuickShopItemExpressionHandler(this.plugin).register(), "QuickShop-Hikari");
        }
        if (this.isPluginEnabled("packetevents") && Config.injectPacketEvents()) {
            runCatchingHook(() -> WrappedBlockStateHelper.register(null), "packetevents");
        }
        if (this.isPluginEnabled("GrimAC") && Config.injectPacketEvents()) {
            runCatchingHook(() -> WrappedBlockStateHelper.register("ac{}grim{}grimac{}shaded{}com{}github{}retrooper{}packetevents"), "GrimAC");
        }
        BukkitLevelerBridge levelerBridge = BukkitLevelerBridge.builder()
                .onHookSuccess(this::logHook)
                .onHookFailure((s, t) -> this.plugin.logger().warn("Failed to hook " + s, t))
                .detectSupportedPlugins()
                .build();
        for (cn.gtemc.levelerbridge.api.LevelerProvider<org.bukkit.entity.Player> provider : levelerBridge.providers()) {
            this.registerLevelerProvider(new LevelerBridgeLeveler(provider));
        }
        BukkitItemBridge itemBridge = BukkitItemBridge.builder()
                .onHookSuccess(this::logHook)
                .onHookFailure((s, t) -> this.plugin.logger().warn("Failed to hook " + s, t))
                .detectSupportedPlugins(p -> !p.getName().equalsIgnoreCase("CraftEngine"))
                .build();
        for (Provider<ItemStack, org.bukkit.entity.Player> provider : itemBridge.providers()) {
            this.registerItemSource(new ItemBridgeSource(provider));
        }
    }

    private void runCatchingHook(ThrowableRunnable runnable, String plugin) {
        try {
            runnable.run();
            logHook(plugin);
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to hook " + plugin, e);
        }
    }

    private interface ThrowableRunnable {
        void run() throws Throwable;
    }

    @Override
    public void registerTagResolverProvider(TagResolverProvider provider) {
        this.tagResolverProviders.put(provider.name(), provider);
        this.tagResolverProviderArray = this.tagResolverProviders.values().toArray(new TagResolverProvider[0]);
        FormattedLine.Companion.resetWithCustomResolvers(new ArrayList<>(this.tagResolverProviders.keySet()));
    }

    private void logHook(String plugin) {
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("info.compatibility", plugin));
    }

    @Override
    public ExternalModel createModel(String id) {
        for (ModelProvider modelProvider : this.modelProviders.values()) {
            ExternalModel model = modelProvider.createModel(id);
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    @Override
    public int interactionToBaseEntity(int id) {
        return ModelEngineUtils.interactionToBaseEntity(id);
    }

    private void initLuckPermsHook() {
        new LuckPermsEventListeners(this.plugin.javaPlugin(), this.plugin.fontManager()::refreshEmojiSuggestions);
        this.hasLuckPerms = true;
    }

    private void initSlimeWorldHook() {
        WorldManager worldManager = this.plugin.worldManager();
        if (VersionHelper.isOrAbove1_21_4()) {
            try {
                Class.forName("com.infernalsuite.asp.api.AdvancedSlimePaperAPI");
                runCatchingHook(() -> {
                    SlimeFormatStorageAdaptor adaptor = new SlimeFormatStorageAdaptor(worldManager);
                    worldManager.setStorageAdaptor(adaptor);
                    Bukkit.getPluginManager().registerEvents(adaptor, this.plugin.javaPlugin());
                }, "AdvancedSlimePaper");
            } catch (ClassNotFoundException ignored) {
            }
        } else {
            try {
                Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
                runCatchingHook(() -> {
                    LegacySlimeFormatStorageAdaptor adaptor = new LegacySlimeFormatStorageAdaptor(worldManager, 1);
                    worldManager.setStorageAdaptor(adaptor);
                    Bukkit.getPluginManager().registerEvents(adaptor, this.plugin.javaPlugin());
                }, "AdvancedSlimePaper");
            } catch (ClassNotFoundException ignored) {
                if (hasPlugin("SlimeWorldPlugin")) {
                    runCatchingHook(() -> {
                        LegacySlimeFormatStorageAdaptor adaptor = new LegacySlimeFormatStorageAdaptor(worldManager, 2);
                        worldManager.setStorageAdaptor(adaptor);
                        Bukkit.getPluginManager().registerEvents(adaptor, this.plugin.javaPlugin());
                    }, "AdvancedSlimePaper");
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    private void initFastAsyncWorldEditHook() {
        Plugin fastAsyncWorldEdit = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        String version = VersionHelper.isPaper() ? fastAsyncWorldEdit.getPluginMeta().getVersion() : fastAsyncWorldEdit.getDescription().getVersion();
        if (!WorldEditBlockRegister.checkFAWECompatible(version)) {
            if (VersionHelper.isOrAbove1_20_3()) {
                this.plugin.logger().error("");
                if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                    this.plugin.logger().error("[兼容性] 插件需要更新 FastAsyncWorldEdit 到 2.13.0 或更高版本，以获得更好的兼容性。(当前版本: " + version + ")");
                    this.plugin.logger().error("[兼容性] 请前往 https://ci.athion.net/job/FastAsyncWorldEdit/ 下载最新版本");
                } else {
                    this.plugin.logger().error("[Compatibility] Update FastAsyncWorldEdit to v2.13.0+ for better compatibility (Current: " + version + ")");
                    this.plugin.logger().error("[Compatibility] Download latest version: https://ci.athion.net/job/FastAsyncWorldEdit/");
                }
                this.plugin.logger().error("");
            }
        }
        WorldEditBlockRegister.init(true);
    }

    private void initWorldEditHook() {
        WorldEditBlockRegister.init(false);
        try {
            for (int i = 0; i < Config.serverSideBlocks(); i++) {
                WorldEditBlockRegister.register(BlockManager.createCustomBlockKey(i));
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to initialize world edit hook", e);
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
        return player == null
                ? PlaceholderAPIUtils.parse(null, text)
                : PlaceholderAPIUtils.parse((org.bukkit.entity.Player) player.platformPlayer(), text);
    }

    @Override
    public String parse(Player player1, Player player2, String text) {
        return PlaceholderAPIUtils.parse((org.bukkit.entity.Player) player1.platformPlayer(), (org.bukkit.entity.Player) player2.platformPlayer(), text);
    }

    @Override
    public int getViaVersionProtocolVersion(NetWorkUser user) {
        return ViaVersionUtils.getPlayerProtocolVersion(user);
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

    @Override
    public boolean isBedrockPlayer(Player player) {
        UUID uuid = player.uuid();
        if (this.hasFloodgate) {
            return FloodgateUtils.isFloodgatePlayer(uuid);
        }
        if (this.hasGeyser) {
            return GeyserUtils.isGeyserPlayer(uuid);
        }
        return uuid.version() == 0;
    }

    @Override
    public boolean hasPermission(NetWorkUser user, String permission) {
        if (user.platformPlayer() instanceof org.bukkit.entity.Player player) {
            return player.hasPermission(permission);
        }
        if (this.hasLuckPerms) {
            return LuckPermsUtils.hasPermission(user, permission);
        }
        return false;
    }
}
