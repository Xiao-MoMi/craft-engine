package net.momirealms.craftengine.core.plugin;

import net.momirealms.craftengine.core.advancement.AdvancementManager;
import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.RecipeManager;
import net.momirealms.craftengine.core.item.recipe.network.legacy.LegacyRecipeTypes;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.RecipeDisplayTypes;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplayTypes;
import net.momirealms.craftengine.core.loot.VanillaLootManager;
import net.momirealms.craftengine.core.pack.PackManager;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManagerImpl;
import net.momirealms.craftengine.core.plugin.context.GlobalVariableManager;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManager;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManagerImpl;
import net.momirealms.craftengine.core.plugin.gui.GuiManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManagerImpl;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.logger.filter.DisconnectLogFilter;
import net.momirealms.craftengine.core.plugin.logger.filter.LogFilter;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.sound.SoundManager;
import net.momirealms.craftengine.core.world.WorldManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public abstract class CraftEngine implements Plugin {
    private static CraftEngine instance;
    protected PluginLogger logger;
    protected Config config;
    protected Platform platform;
    protected ClassPathAppender classPathAppender;
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
    }

    public static CraftEngine instance() {
        if (instance == null) {
            throw new IllegalStateException("CraftEngine has not been initialized");
        }
        return instance;
    }

    protected void onPluginLoad() {
        RecipeDisplayTypes.register();
        SlotDisplayTypes.register();
        LegacyRecipeTypes.register();
        ((Logger) LogManager.getRootLogger()).addFilter(new LogFilter());
        ((Logger) LogManager.getRootLogger()).addFilter(new DisconnectLogFilter());
    }

    public record ReloadResult(boolean success, long asyncTime, long syncTime) {

        static ReloadResult failure() {
            return new ReloadResult(false, -1L, -1L);
        }

        static ReloadResult success(long asyncTime, long syncTime) {
            return new ReloadResult(true, asyncTime, syncTime);
        }
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
                // firstly reload main config
                this.config.load();
                // now we reload the translations
                this.translationManager.reload();
                // clear the outdated cache by reloading the managers
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
                if (reloadRecipe) {
                    this.recipeManager.reload();
                }
                try {
                    // now we load resources
                    this.packManager.loadResources(reloadRecipe);
                } catch (Exception e) {
                    this.logger().warn("Failed to load resources folder", e);
                }
                // register trims
                this.itemManager.delayedLoad();
                // init suggestions and packet mapper
                this.blockManager.delayedLoad();
                // handle some special client lang for instance block_name
                this.translationManager.delayedLoad();
                // init suggestions
                this.furnitureManager.delayedLoad();
                // sort the categories
                this.itemBrowserManager.delayedLoad();
                // collect illegal characters from minecraft:default font
                this.fontManager.delayedLoad();
                this.advancementManager.delayedLoad();
                if (reloadRecipe) {
                    // convert data pack recipes
                    this.recipeManager.delayedLoad();
                }
                long time2 = System.currentTimeMillis();
                asyncTime = time2 - time1;
            } finally {
                long finalAsyncTime = asyncTime;
                syncExecutor.execute(() -> {
                    try {
                        long time3 = System.currentTimeMillis();
                        // register songs
                        this.soundManager.runDelayedSyncTasks();
                        // register recipes
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
        this.networkManager.init();
        this.templateManager = new TemplateManagerImpl();
        this.globalVariableManager = new GlobalVariableManager();
        this.itemBrowserManager = new ItemBrowserManagerImpl(this);
        this.commandManager.registerDefaultFeatures();
        // delay the reload so other plugins can register some custom parsers
        this.scheduler.sync().runDelayed(() -> {
            this.registerDefaultParsers();
            // hook external item plugins
            this.itemManager.delayedInit();
            // hook worldedit
            this.blockManager.delayedInit();
            // register listeners and tasks
            this.guiManager.delayedInit();
            this.recipeManager.delayedInit();
            this.packManager.delayedInit();
            this.fontManager.delayedInit();
            this.vanillaLootManager.delayedInit();
            this.advancementManager.delayedInit();
            // reload the plugin
            try {
                this.reloadPlugin(Runnable::run, Runnable::run, true);
            } catch (Exception e) {
                this.logger.warn("Failed to reload plugin on enable stage", e);
            }
            // must be after reloading because this process loads furniture
            this.projectileManager.delayedInit();
            this.worldManager.delayedInit();
            this.furnitureManager.delayedInit();
            // set up some platform extra tasks
            this.platformDelayedEnable();
            this.isInitializing = false;
            this.scheduler.executeAsync(() -> this.packManager.initCachedAssets());
        });
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
        if (this.translationManager != null) this.translationManager.disable();
        if (this.globalVariableManager != null) this.globalVariableManager.disable();
        if (this.projectileManager != null) this.projectileManager.disable();
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
        this.packManager.registerConfigSectionParser(this.blockManager.parser());
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
                Dependencies.COMMONS_IO, Dependencies.COMMONS_LANG3, Dependencies.COMMONS_IMAGING,
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
                Dependencies.JIMFS
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <W> SchedulerAdapter<W> scheduler() {
        return (SchedulerAdapter<W>) scheduler;
    }

    @Override
    public ClassPathAppender classPathAppender() {
        return classPathAppender;
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
    public Platform platform() {
        return platform;
    }
}
