package net.momirealms.craftengine.core.plugin;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.advancement.AdvancementManager;
import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.entity.seat.SeatManager;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.RecipeManager;
import net.momirealms.craftengine.core.item.recipe.network.legacy.LegacyRecipeTypes;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.RecipeDisplayTypes;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplayTypes;
import net.momirealms.craftengine.core.loot.VanillaLootManager;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.PackManager;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.compatibility.PluginTaskRegistry;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.context.GlobalVariableManager;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManager;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManagerImpl;
import net.momirealms.craftengine.core.entity.culling.EntityCullingManager;
import net.momirealms.craftengine.core.entity.culling.EntityCullingManagerImpl;
import net.momirealms.craftengine.core.plugin.gui.GuiManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManagerImpl;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.locale.TranslationManagerImpl;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.logger.filter.DisconnectLogFilter;
import net.momirealms.craftengine.core.plugin.logger.filter.LogFilter;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.sound.SoundManager;
import net.momirealms.craftengine.core.util.CompletableFutures;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.score.TeamManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public abstract class CraftEngine implements Plugin {
    private static CraftEngine instance;
    protected PluginLogger logger;
    protected Config config;
    protected Platform platform;
    protected ClassPathAppender sharedClassPathAppender;
    protected ClassPathAppender privateClassPathAppender;
    protected DependencyManager dependencyManager;
    protected SchedulerAdapter<?> scheduler;
    protected NetworkManager networkManager;
    protected FontManager fontManager;
    protected PackManager packManager;
    protected ItemManager<?> itemManager;
    protected RecipeManager<?> recipeManager;
    protected BlockManager blockManager;
    protected TranslationManager translationManager;
    protected WorldManager worldManager;
    protected FurnitureManager furnitureManager;
    protected CraftEngineCommandManager<?> commandManager;
    protected SenderFactory<? extends Plugin, ?> senderFactory;
    protected TemplateManager templateManager;
    protected ItemBrowserManager itemBrowserManager;
    protected GuiManager guiManager;
    protected SoundManager soundManager;
    protected VanillaLootManager vanillaLootManager;
    protected AdvancementManager advancementManager;
    protected CompatibilityManager compatibilityManager;
    protected GlobalVariableManager globalVariableManager;
    protected ProjectileManager projectileManager;
    protected SeatManager seatManager;
    protected EntityCullingManager entityCullingManager;
    protected TeamManager teamManager;

    private final PluginTaskRegistry beforeEnableTaskRegistry = new PluginTaskRegistry();
    private final PluginTaskRegistry afterEnableTaskRegistry = new PluginTaskRegistry();

    private final Consumer<CraftEngine> reloadEventDispatcher;
    private boolean isReloading;
    private boolean isInitializing;

    private String buildByBit = "%%__BUILTBYBIT__%%";
    private String polymart = "%%__POLYMART__%%";
    private String time = "%%__TIMESTAMP__%%";
    private String user = "%%__USER__%%";
    private String username = "%%__USERNAME__%%";

    protected CraftEngine(Consumer<CraftEngine> reloadEventDispatcher) {
        instance = this;
        this.reloadEventDispatcher = reloadEventDispatcher;
        ((Logger) LogManager.getRootLogger()).addFilter(new LogFilter());
        ((Logger) LogManager.getRootLogger()).addFilter(new DisconnectLogFilter());
    }

    public static CraftEngine instance() {
        if (instance == null) {
            throw new IllegalStateException("CraftEngine has not been initialized");
        }
        return instance;
    }

    protected void onPluginLoad() {
        RecipeDisplayTypes.init();
        SlotDisplayTypes.init();
        LegacyRecipeTypes.init();

        // 初始化模板管理器
        this.templateManager = TemplateManager.INSTANCE;
        // 初始化全局变量管理器
        this.globalVariableManager = new GlobalVariableManager();
        // 初始化物品浏览器
        this.itemBrowserManager = new ItemBrowserManagerImpl(this);
        // 初始化实体剔除器
        this.entityCullingManager = new EntityCullingManagerImpl();
    }

    public void setUpConfigAndLocale() {
        this.config = new Config(this);
        this.config.updateConfigCache();
        // 先读取语言后，再重载语言文件系统
        this.config.loadForcedLocale();
        this.translationManager = new TranslationManagerImpl(this);
        this.translationManager.reload();
        // 最后才加载完整的config配置
        this.config.loadFullSettings();
    }

    public record ReloadResult(boolean success, long asyncTime, long syncTime) {

        static ReloadResult failure() {
            return new ReloadResult(false, -1L, -1L);
        }

        static ReloadResult success(long asyncTime, long syncTime) {
            return new ReloadResult(true, asyncTime, syncTime);
        }
    }

    private void reloadManagers() {
        this.templateManager.reload();
        this.globalVariableManager.reload();
        this.furnitureManager.reload();
        this.fontManager.reload();
        this.itemManager.reload();
        this.soundManager.reload();
        this.itemBrowserManager.reload();
        this.blockManager.reload();
        this.worldManager.reload();
        this.vanillaLootManager.reload();
        this.guiManager.reload();
        this.packManager.reload();
        this.advancementManager.reload();
        this.projectileManager.reload();
        this.seatManager.reload();
        this.networkManager.reload();
    }

    private void runDelayTasks(boolean reloadRecipe) {
        List<CompletableFuture<Void>> delayedLoadTasks = new ArrayList<>();
        // 指令补全，重置外部配方原料
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.itemManager.delayedLoad(), this.scheduler.async()));
        // 重置映射表，指令补全，发送tags，收集声音
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.blockManager.delayedLoad(), this.scheduler.async()));
        // 处理block_name特殊语言键
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.translationManager.delayedLoad(), this.scheduler.async()));
        // 指令补全
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.furnitureManager.delayedLoad(), this.scheduler.async()));
        // 处理外部category，加载ui常量
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.itemBrowserManager.delayedLoad(), this.scheduler.async()));
        // 收集非法字符，构造前缀树，指令补全
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.fontManager.delayedLoad(), this.scheduler.async()));
        // 指令补全
        delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.soundManager.delayedLoad(), this.scheduler.async()));
        // 如果重载配方
        if (reloadRecipe) {
            // 转换数据包配方
            delayedLoadTasks.add(CompletableFuture.runAsync(() -> this.recipeManager.delayedLoad(), this.scheduler.async()));
        }
        CompletableFutures.allOf(delayedLoadTasks).join();
    }

    public CompletableFuture<ReloadResult> reloadPlugin(Executor asyncExecutor, Executor syncExecutor, boolean reloadRecipe) {
        CompletableFuture<ReloadResult> future = new CompletableFuture<>();
        asyncExecutor.execute(() -> {
            long asyncTime = -1;
            try {
                if (this.isReloading) {
                    future.complete(ReloadResult.failure());
                    return;
                }
                this.isReloading = true;
                long time1 = System.currentTimeMillis();
                // 重载config
                this.config.load();
                // 重载翻译
                this.translationManager.reload();
                // 重载其他管理器
                this.reloadManagers();
                if (reloadRecipe) {
                    this.recipeManager.reload();
                }
                try {
                    // 加载全部配置资源
                    this.packManager.loadPacks();
                    this.packManager.updateCachedConfigFiles();
                    this.packManager.loadResources(reloadRecipe ? (p) -> true : (p) -> p.loadingSequence() != LoadingSequence.RECIPE);
                    this.packManager.clearResourceConfigs();
                } catch (Exception e) {
                    this.logger().warn("Failed to load resources folder", e);
                }
                // 执行延迟任务
                this.runDelayTasks(reloadRecipe);
                // 重新发送tags，需要等待tags更新完成
                this.networkManager.delayedLoad();
                long time2 = System.currentTimeMillis();
                asyncTime = time2 - time1;
            } finally {
                long finalAsyncTime = asyncTime;
                syncExecutor.execute(() -> {
                    try {
                        long time3 = System.currentTimeMillis();
                        // 注册唱片机音乐
                        this.soundManager.runDelayedSyncTasks();
                        // 同步注册配方
                        if (reloadRecipe) {
                            this.recipeManager.runDelayedSyncTasks();
                        }
                        long time4 = System.currentTimeMillis();
                        long syncTime = time4 - time3;
                        this.reloadEventDispatcher.accept(this);
                        future.complete(ReloadResult.success(finalAsyncTime, syncTime));
                    } finally {
                        this.isReloading = false;
                    }
                });
            }
        });
        return future;
    }

    protected void onPluginEnable() {
        this.isInitializing = true;

        // 注册网络相关的bukkit事件监听器
        this.networkManager.init();
        // 注册指令
        this.commandManager.registerDefaultFeatures();
        // 注册物品相关的事件监听器
        this.itemManager.delayedInit();
        // 注册方块相关的事件监听器
        this.blockManager.delayedInit();
        // 注册容器相关的监听器
        this.guiManager.delayedInit();
        // 注册配方相关的监听器
        this.recipeManager.delayedInit();
        // 注册数据包状态的监听器
        this.packManager.delayedInit();
        // 注册聊天监听器
        this.fontManager.delayedInit();
        // 注册实体死亡监听器
        this.vanillaLootManager.delayedInit();
        // 注册脱离坐骑监听器
        this.seatManager.delayedInit();
        // 加载实体剔除线程
        this.entityCullingManager.load();

        if (!Config.delayConfigurationLoad()) {
            // 注册世界加载相关监听器
            this.worldManager.delayedInit();
        }

        // 延迟任务
        this.beforeEnableTaskRegistry.executeTasks();

        if (!Config.delayConfigurationLoad()) {
            // 清理缓存，初始化一些东西，不需要读config和translation，因为boostrap阶段已经读取过了
            this.reloadManagers();
            // 加载packs
            this.packManager.loadPacks();
            this.packManager.updateCachedConfigFiles();
            // 不要加载配方
            this.packManager.loadResources((p) -> p.loadingSequence() != LoadingSequence.RECIPE);
            this.runDelayTasks(false);
        }

        // 延迟任务
        this.afterEnableTaskRegistry.executeTasks();

        // 延迟重载，以便其他依赖CraftEngine的插件能注册parser
        this.scheduler.sync().runDelayed(() -> {
            // 初始化一些平台的任务
            this.platformDelayedEnable();

            // 延迟兼容性任务，比如物品库的支持。保证后续配方正确加载
            this.compatibilityManager.onDelayedEnable();

            if (!Config.delayConfigurationLoad()) {
                // 单独加载配方
                this.recipeManager.reload();
                this.packManager.loadResources((p) -> p.loadingSequence() == LoadingSequence.RECIPE);
                this.recipeManager.delayedLoad();
                this.packManager.clearResourceConfigs();
                // 重新发送tags，需要等待tags更新完成
                this.networkManager.delayedLoad();
                // 注册唱片机音乐
                this.soundManager.runDelayedSyncTasks();
                // 同步注册配方
                this.recipeManager.runDelayedSyncTasks();
            } else {
                try {
                    this.reloadPlugin(Runnable::run, Runnable::run, true);
                    this.worldManager.delayedInit();
                } catch (Exception e) {
                    this.logger.severe("Failed to reload plugin on delayed enable stage", e);
                }
            }

            // 必须要在完整重载后再初始化，否则会因为配置不存在，导致家具、弹射物等无法正确被加载
            this.projectileManager.delayedInit();
            this.furnitureManager.delayedInit();
            // 完成初始化
            this.isInitializing = false;
            // 异步去缓存资源包相关文件
            this.scheduler.executeAsync(() -> this.packManager.initCachedAssets());
            // 正式完成重载
            this.reloadEventDispatcher.accept(this);
            // 检查更新
            if (Config.checkUpdate()) {
                this.scheduler.executeAsync(this::checkUpdates);
            }
        });
    }

    private void checkUpdates() {
        boolean downloadFromPolymart = this.polymart.equals("1");
        boolean downloadFromBBB = this.buildByBit.equals("true");
        String link;
        if (VersionHelper.PREMIUM) {
            if (downloadFromPolymart) {
                link = "https://polymart.org/product/7624/";
            } else if (downloadFromBBB) {
                link = "https://builtbybit.com/resources/82674/";
            } else {
                if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                    link = "QQ群[1039968907]";
                } else {
                    return;
                }
            }
        } else {
            link = "https://modrinth.com/plugin/craftengine/";
        }
        try {
            String lv = getLatestVersion();
            if (lv == null) return;
            if (compareVer(lv, pluginVersion())) {
                this.logger.warn(TranslationManager.instance().translateLog("info.update.available", lv, link));
            } else {
                this.logger.info(TranslationManager.instance().translateLog("info.update.latest"));
            }
        } catch (Exception ignored) {
        }
    }

    private boolean compareVer(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 > num2;
            }
        }
        return false;
    }

    @Nullable
    private static String getLatestVersion() throws Exception {
        String apiUrl = "https://api.spiget.org/v2/resources/128871/versions/latest";
        URL url = new URI(apiUrl).toURL();
        // 创建HTTP连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        // 获取响应代码
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JsonObject jsonResponse = GsonHelper.get().fromJson(response.toString(), JsonObject.class);
            if (jsonResponse.has("name")) {
                return jsonResponse.get("name").getAsString();
            }
        }
        return null;
    }

    protected void onPluginDisable() {
        if (this.networkManager != null) this.networkManager.disable();
        if (this.fontManager != null) this.fontManager.disable();
        if (this.advancementManager != null) this.advancementManager.disable();
        if (this.packManager != null) this.packManager.disable();
        if (this.itemManager != null) this.itemManager.disable();
        if (this.blockManager != null) this.blockManager.disable();
        if (this.furnitureManager != null) this.furnitureManager.disable();
        if (this.templateManager != null) this.templateManager.disable();
        if (this.worldManager != null) this.worldManager.disable();
        if (this.recipeManager != null) this.recipeManager.disable();
        if (this.itemBrowserManager != null) this.itemBrowserManager.disable();
        if (this.guiManager != null) this.guiManager.disable();
        if (this.soundManager != null) this.soundManager.disable();
        if (this.vanillaLootManager != null) this.vanillaLootManager.disable();
        if (this.seatManager != null) this.seatManager.disable();
        if (this.translationManager != null) this.translationManager.disable();
        if (this.globalVariableManager != null) this.globalVariableManager.disable();
        if (this.projectileManager != null) this.projectileManager.disable();
        if (this.entityCullingManager != null) this.entityCullingManager.disable();
        if (this.scheduler != null) this.scheduler.shutdownScheduler();
        if (this.scheduler != null) this.scheduler.shutdownExecutor();
        if (this.commandManager != null) this.commandManager.unregisterFeatures();
        if (this.senderFactory != null) this.senderFactory.close();
        if (this.dependencyManager != null) this.dependencyManager.close();
    }

    protected void registerDefaultParsers() {
        // register template parser
        this.packManager.registerConfigSectionParser(this.templateManager.parser());
        // register global variables parser
        this.packManager.registerConfigSectionParser(this.globalVariableManager.parser());
        // register font parser
        this.packManager.registerConfigSectionParsers(this.fontManager.parsers());
        // register item parser
        this.packManager.registerConfigSectionParsers(this.itemManager.parsers());
        // register furniture parser
        this.packManager.registerConfigSectionParser(this.furnitureManager.parser());
        // register block parser
        this.packManager.registerConfigSectionParsers(this.blockManager.parsers());
        // register recipe parser
        this.packManager.registerConfigSectionParser(this.recipeManager.parser());
        // register category parser
        this.packManager.registerConfigSectionParser(this.itemBrowserManager.parser());
        // register translation parser
        this.packManager.registerConfigSectionParsers(this.translationManager.parsers());
        // register sound parser
        this.packManager.registerConfigSectionParsers(this.soundManager.parsers());
        // register vanilla loot parser
        this.packManager.registerConfigSectionParser(this.vanillaLootManager.parser());
        // register advancement parser
        this.packManager.registerConfigSectionParser(this.advancementManager.parser());
        // register skip-optimization parser
        this.packManager.registerConfigSectionParser(this.packManager.parser());
        // register feature parser
        this.packManager.registerConfigSectionParsers(this.worldManager.parsers());
    }

    public void applyDependencies() {
        this.dependencyManager = new DependencyManagerImpl(this);
        ArrayList<Dependency> dependenciesToLoad = new ArrayList<>();
        dependenciesToLoad.addAll(commonDependencies());
        dependenciesToLoad.addAll(platformDependencies());
        this.dependencyManager.loadDependencies(dependenciesToLoad);
    }

    protected abstract void platformDelayedEnable();

    protected abstract List<Dependency> platformDependencies();

    protected List<Dependency> commonDependencies() {
        return List.of(
                Dependencies.BSTATS_BASE,
                Dependencies.CAFFEINE,
                Dependencies.GEANTY_REF,
                Dependencies.CLOUD_CORE, Dependencies.CLOUD_SERVICES,
                Dependencies.GSON,
                Dependencies.COMMONS_IO, Dependencies.COMMONS_LANG3,
                Dependencies.ZSTD,
                Dependencies.BYTE_BUDDY, Dependencies.BYTE_BUDDY_AGENT,
                Dependencies.SNAKE_YAML,
                Dependencies.BOOSTED_YAML,
                Dependencies.OPTION,
                Dependencies.EXAMINATION_API, Dependencies.EXAMINATION_STRING,
                Dependencies.ADVENTURE_KEY, Dependencies.ADVENTURE_API, Dependencies.ADVENTURE_NBT,
                Dependencies.MINIMESSAGE,
                Dependencies.TEXT_SERIALIZER_COMMONS, Dependencies.TEXT_SERIALIZER_LEGACY, Dependencies.TEXT_SERIALIZER_GSON, Dependencies.TEXT_SERIALIZER_GSON_LEGACY, Dependencies.TEXT_SERIALIZER_JSON,
                Dependencies.AHO_CORASICK,
                Dependencies.LZ4,
                Dependencies.EVALEX,
                Dependencies.NETTY_HTTP,
                Dependencies.JIMFS,
                Dependencies.BUCKET_4_J
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <W> SchedulerAdapter<W> scheduler() {
        return (SchedulerAdapter<W>) scheduler;
    }

    @Override
    public ClassPathAppender sharedClassPathAppender() {
        return sharedClassPathAppender;
    }

    @Override
    public ClassPathAppender privateClassPathAppender() {
        return privateClassPathAppender;
    }

    @Override
    public Config config() {
        return config;
    }

    @Override
    public PluginLogger logger() {
        return logger;
    }

    @Override
    public boolean isReloading() {
        return isReloading;
    }

    @Override
    public boolean isInitializing() {
        return isInitializing;
    }

    @Override
    public DependencyManager dependencyManager() {
        return dependencyManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ItemManager<T> itemManager() {
        return (ItemManager<T>) itemManager;
    }

    @Override
    public BlockManager blockManager() {
        return blockManager;
    }

    @Override
    public NetworkManager networkManager() {
        return networkManager;
    }

    @Override
    public FontManager fontManager() {
        return fontManager;
    }

    @Override
    public AdvancementManager advancementManager() {
        return advancementManager;
    }

    @Override
    public TranslationManager translationManager() {
        return translationManager;
    }

    @Override
    public TemplateManager templateManager() {
        return templateManager;
    }

    @Override
    public FurnitureManager furnitureManager() {
        return furnitureManager;
    }

    @Override
    public PackManager packManager() {
        return packManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> RecipeManager<T> recipeManager() {
        return (RecipeManager<T>) recipeManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends Plugin, C> SenderFactory<P, C> senderFactory() {
        return (SenderFactory<P, C>) senderFactory;
    }

    @Override
    public WorldManager worldManager() {
        return worldManager;
    }

    @Override
    public ItemBrowserManager itemBrowserManager() {
        return itemBrowserManager;
    }

    @Override
    public GuiManager guiManager() {
        return guiManager;
    }

    @Override
    public SoundManager soundManager() {
        return soundManager;
    }

    @Override
    public VanillaLootManager vanillaLootManager() {
        return vanillaLootManager;
    }

    @Override
    public CompatibilityManager compatibilityManager() {
        return compatibilityManager;
    }

    @Override
    public GlobalVariableManager globalVariableManager() {
        return globalVariableManager;
    }

    @Override
    public ProjectileManager projectileManager() {
        return projectileManager;
    }

    @Override
    public EntityCullingManager entityCullingManager() {
        return entityCullingManager;
    }

    @Override
    public TeamManager teamManager() {
        return teamManager;
    }

    @Override
    public SeatManager seatManager() {
        return seatManager;
    }

    @Override
    public Platform platform() {
        return platform;
    }

    /**
     *
     * This task registry allows you to schedule tasks to run before CraftEngine enable, without dealing with plugin dependencies.
     * You must register these tasks during the onLoad phase; otherwise, they will not be executed.
     *
     * @return PluginTaskRegistry
     */
    @ApiStatus.Experimental
    public PluginTaskRegistry beforeEnableTaskRegistry() {
        return beforeEnableTaskRegistry;
    }

    /**
     *
     * This task registry allows you to schedule tasks to run after CraftEngine enable, without dealing with plugin dependencies.
     * You must register these tasks during the onLoad phase; otherwise, they will not be executed.
     *
     * @return PluginTaskRegistry
     */
    @ApiStatus.Experimental
    public PluginTaskRegistry afterEnableTaskRegistry() {
        return afterEnableTaskRegistry;
    }
}
