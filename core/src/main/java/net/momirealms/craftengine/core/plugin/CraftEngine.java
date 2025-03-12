package net.momirealms.craftengine.core.plugin;

import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.font.ImageManager;
import net.momirealms.craftengine.core.font.ImageManagerImpl;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.RecipeManager;
import net.momirealms.craftengine.core.pack.PackManager;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManagerImpl;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManager;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManagerImpl;
import net.momirealms.craftengine.core.plugin.gui.GuiManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManagerImpl;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.locale.TranslationManagerImpl;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.logger.filter.LogFilter;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.sound.SoundManager;
import net.momirealms.craftengine.core.world.WorldManager;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class CraftEngine implements Plugin {
    public static final String MOD_CLASS = "net.momirealms.craftengine.mod.CraftEnginePlugin";
    public static final String NAMESPACE = "craftengine";
    private static CraftEngine instance;
    protected DependencyManager dependencyManager;
    protected SchedulerAdapter<?> scheduler;
    protected NetworkManager networkManager;
    protected ClassPathAppender classPathAppender;
    protected ImageManager imageManager;
    protected PackManager packManager;
    protected ConfigManager configManager;
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

    protected PluginLogger logger;
    protected Consumer<Supplier<String>> debugger = (s) -> {};
    private boolean isReloading;

    protected CraftEngine() {
        instance = this;
    }

    @Override
    public void load() {
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addFilter(new LogFilter());
        this.dependencyManager = new DependencyManagerImpl(this);
        ArrayList<Dependency> dependenciesToLoad = new ArrayList<>();
        dependenciesToLoad.addAll(commonDependencies());
        dependenciesToLoad.addAll(platformDependencies());
        this.dependencyManager.loadDependencies(dependenciesToLoad);
        this.translationManager = new TranslationManagerImpl(this);
        this.configManager = new ConfigManager(this);
    }

    @Override
    public void reload() {
        if (this.isReloading) return;
        this.isReloading = true;
        try {
            this.configManager.reload();
            this.translationManager.reload();
            this.templateManager.reload();
            this.furnitureManager.reload();
            this.imageManager.reload();
            this.itemManager.reload();
            this.soundManager.reload();
            this.recipeManager.reload();
            this.itemBrowserManager.reload();
            this.blockManager.reload();
            this.worldManager.reload();
            this.packManager.reload();
            this.guiManager.reload();
            this.blockManager.delayedLoad();
            this.itemBrowserManager.delayedLoad();
            this.soundManager.delayedLoad();
            if (ConfigManager.debug()) {
                this.debugger = (s) -> logger.info("[Debug] " + s.get());
            } else {
                this.debugger = (s) -> {};
            }
        } finally {
            this.recipeManager.delayedLoad().thenRun(() -> this.isReloading = false);
        }
    }

    @Override
    public void enable() {
        this.networkManager.enable();
        this.imageManager = new ImageManagerImpl(this);
        this.templateManager = new TemplateManagerImpl(this);
        this.itemBrowserManager = new ItemBrowserManagerImpl(this);
        this.commandManager.registerDefaultFeatures();
        // delay the reload so other plugins can register some parsers
        this.scheduler.sync().runDelayed(() -> {
            this.registerParsers();
            this.reload();
            this.guiManager.delayedInit();
            this.recipeManager.delayedInit();
            this.blockManager.delayedInit();
            this.itemManager.delayedInit();
            this.worldManager.delayedInit();
            this.packManager.delayedInit();
            this.furnitureManager.delayedInit();
        });
    }

    @Override
    public void disable() {
        if (this.senderFactory != null) this.senderFactory.close();
        if (this.commandManager != null) this.commandManager.unregisterFeatures();
        if (this.networkManager != null) this.networkManager.shutdown();
        if (this.imageManager != null) this.imageManager.disable();
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
        if (this.scheduler != null) this.scheduler.shutdownScheduler();
        if (this.scheduler != null) this.scheduler.shutdownExecutor();
        ResourcePackHost.instance().disable();
    }

    protected abstract void registerParsers();

    protected abstract List<Dependency> platformDependencies();

    protected List<Dependency> commonDependencies() {
        return List.of(
                Dependencies.BSTATS_BASE,
                Dependencies.CAFFEINE,
                Dependencies.GEANTY_REF,
                Dependencies.NETTY_HTTP,
                Dependencies.CLOUD_CORE, Dependencies.CLOUD_SERVICES,
                Dependencies.GSON,
                Dependencies.SLF4J_API, Dependencies.SLF4J_SIMPLE,
                Dependencies.COMMONS_IO,
                Dependencies.ZSTD,
                Dependencies.BYTE_BUDDY,
                Dependencies.SNAKE_YAML,
                Dependencies.BOOSTED_YAML,
                Dependencies.MINIMESSAGE,
                Dependencies.TEXT_SERIALIZER_GSON,
                Dependencies.TEXT_SERIALIZER_JSON
        );
    }

    @Override
    public DependencyManager dependencyManager() {
        return dependencyManager;
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
    public ImageManager imageManager() {
        return imageManager;
    }

    @Override
    public ConfigManager configManager() {
        return configManager;
    }

    @Override
    public PluginLogger logger() {
        return logger;
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

    @Override
    public SenderFactory<? extends Plugin, ?> senderFactory() {
        return senderFactory;
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
    public void debug(Supplier<String> message) {
        debugger.accept(message);
    }

    public boolean isReloading() {
        return isReloading;
    }

    public static CraftEngine instance() {
        if (instance == null) {
            throw new IllegalStateException("CraftEngine has not been initialized");
        }
        return instance;
    }

    public abstract boolean hasPlaceholderAPI();
}
