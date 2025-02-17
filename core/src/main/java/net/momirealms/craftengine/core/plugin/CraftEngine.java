package net.momirealms.craftengine.core.plugin;

import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.font.FontManagerImpl;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.RecipeManager;
import net.momirealms.craftengine.core.pack.PackManager;
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
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.locale.TranslationManagerImpl;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.logger.filter.LogFilter;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.world.WorldManager;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

public abstract class CraftEngine implements Plugin {
    public static final String MOD_CLASS = "net.momirealms.craftengine.mod.CraftEnginePlugin";
    public static final String NAMESPACE = "craftengine";
    private static CraftEngine instance;
    protected DependencyManager dependencyManager;
    protected SchedulerAdapter<?> scheduler;
    protected NetworkManager networkManager;
    protected ClassPathAppender classPathAppender;
    protected FontManager fontManager;
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
    protected PluginLogger logger;
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
            this.translationManager.reload();
            this.configManager.reload();
            this.templateManager.reload();
            this.furnitureManager.reload();
            this.fontManager.reload();
            this.itemManager.reload();
            this.recipeManager.reload();
            this.blockManager.reload();
            this.worldManager.reload();
            this.packManager.reload();
            this.blockManager.delayedLoad();
        } finally {
            this.recipeManager.delayedLoad().thenRunAsync(() -> {
                try {
                    this.packManager.generateResourcePack();
                } finally {
                    this.isReloading = false;
                }
            }, this.scheduler.async());
        }
    }

    @Override
    public void enable() {
        this.networkManager.enable();
        this.fontManager = new FontManagerImpl(this);
        this.templateManager = new TemplateManagerImpl(this);
        this.commandManager.registerDefaultFeatures();
        // delay the reload so other plugins can register some parsers
        this.scheduler.sync().runDelayed(() -> {
            this.registerParsers();
            this.reload();
            this.worldManager.delayedLoad();
            this.furnitureManager.delayedLoad();
        });
    }

    @Override
    public void disable() {
        if (this.senderFactory != null) this.senderFactory.close();
        if (this.commandManager != null) this.commandManager.unregisterFeatures();
        if (this.networkManager != null) this.networkManager.shutdown();
        if (this.fontManager != null) this.fontManager.disable();
        if (this.packManager != null) this.packManager.disable();
        if (this.itemManager != null) this.itemManager.disable();
        if (this.blockManager != null) this.blockManager.disable();
        if (this.furnitureManager != null) this.furnitureManager.disable();
        if (this.templateManager != null) this.templateManager.disable();
        if (this.worldManager != null) this.worldManager.disable();
        if (this.scheduler != null) this.scheduler.shutdownScheduler();
        if (this.scheduler != null) this.scheduler.shutdownExecutor();
    }

    protected abstract void registerParsers();

    protected abstract List<Dependency> platformDependencies();

    protected List<Dependency> commonDependencies() {
        return List.of(
                Dependencies.BSTATS_BASE,
                Dependencies.CAFFEINE,
                Dependencies.GEANTY_REF,
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

    @Override
    public SchedulerAdapter<?> scheduler() {
        return scheduler;
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
    public FontManager fontManager() {
        return fontManager;
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
    public SenderFactory<? extends Plugin, ?> senderFactory() {
        return senderFactory;
    }

    public WorldManager worldManager() {
        return worldManager;
    }

    public static CraftEngine instance() {
        if (instance == null) {
            throw new IllegalStateException("CraftEngine has not been initialized");
        }
        return instance;
    }
}
