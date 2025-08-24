package net.momirealms.craftengine.bukkit.plugin;

import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.craftengine.bukkit.advancement.BukkitAdvancementManager;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehaviors;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.hitbox.BukkitHitBoxTypes;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitProjectileManager;
import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.BukkitItemBehaviors;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.loot.BukkitVanillaLootManager;
import net.momirealms.craftengine.bukkit.pack.BukkitPackManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitSenderFactory;
import net.momirealms.craftengine.bukkit.plugin.gui.BukkitGuiManager;
import net.momirealms.craftengine.bukkit.plugin.injector.*;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.network.PacketConsumers;
import net.momirealms.craftengine.bukkit.plugin.scheduler.BukkitSchedulerAdapter;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.sound.BukkitSoundManager;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.ReflectionClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManagerImpl;
import net.momirealms.craftengine.core.plugin.locale.TranslationManagerImpl;
import net.momirealms.craftengine.core.plugin.logger.JavaPluginLogger;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class BukkitCraftEngine extends CraftEngine {
    private static final String COMPATIBILITY_CLASS = "net.momirealms.craftengine.bukkit.compatibility.BukkitCompatibilityManager";
    private static BukkitCraftEngine instance;
    private SchedulerTask tickTask;
    private boolean successfullyLoaded = false;
    private boolean successfullyEnabled = false;
    private AntiGriefLib antiGrief;
    private JavaPlugin javaPlugin;
    private final Path dataFolderPath;

    protected BukkitCraftEngine(JavaPlugin plugin) {
        this(new JavaPluginLogger(plugin.getLogger()), plugin.getDataFolder().toPath().toAbsolutePath(), new ReflectionClassPathAppender(plugin.getClass().getClassLoader()));
        this.setJavaPlugin(plugin);
    }

    protected BukkitCraftEngine(PluginLogger logger, Path dataFolderPath, ClassPathAppender classPathAppender) {
        super((p) -> {
            CraftEngineReloadEvent event = new CraftEngineReloadEvent((BukkitCraftEngine) p);
            EventUtils.fireAndForget(event);
        });
        instance = this;
        this.dataFolderPath = dataFolderPath;
        super.classPathAppender = classPathAppender;
        super.logger = logger;
        super.platform = new BukkitPlatform();
        super.scheduler = new BukkitSchedulerAdapter(this);
        Class<?> compatibilityClass = Objects.requireNonNull(ReflectionUtils.getClazz(COMPATIBILITY_CLASS), "Compatibility class not found");
        try {
            super.compatibilityManager = (CompatibilityManager) Objects.requireNonNull(ReflectionUtils.getConstructor(compatibilityClass, 0)).newInstance(this);
        } catch (ReflectiveOperationException e) {
            logger().warn("Compatibility class could not be instantiated: " + compatibilityClass.getName());
        }
    }

    protected void setJavaPlugin(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    protected void setUpConfig() {
        this.translationManager = new TranslationManagerImpl(this);
        this.config = new Config(this);
    }

    public void injectRegistries() {
        if (super.blockManager != null) return;
        try {
            BlockGenerator.init();
            BlockStateGenerator.init();
            super.blockManager = new BukkitBlockManager(this);
        } catch (Exception e) {
            throw new InjectionException("Error injecting blocks", e);
        }
        try {
            LootEntryInjector.init();
        } catch (Exception e) {
            throw new InjectionException("Error injecting loot entries", e);
        }
    }

    @Override
    public void onPluginLoad() {
        if (super.blockManager == null) {
            this.injectRegistries();
        }
        try {
            WorldStorageInjector.init();
        } catch (Exception e) {
            throw new InjectionException("Error injecting world storage", e);
        }
        try {
            RecipeInjector.init();
        } catch (Exception e) {
            throw new InjectionException("Error injecting recipes", e);
        }
        try {
            ProtectedFieldVisitor.init();
        } catch (Exception e) {
            throw new InjectionException("Error initializing ProtectedFieldVisitor", e);
        }
        super.onPluginLoad();
        super.blockManager.init();
        super.networkManager = new BukkitNetworkManager(this);
        super.itemManager = new BukkitItemManager(this);
        this.successfullyLoaded = true;
        super.compatibilityManager().onLoad();
    }

    @Override
    protected List<Dependency> platformDependencies() {
        return List.of(
                Dependencies.BSTATS_BUKKIT,
                Dependencies.CLOUD_BUKKIT, Dependencies.CLOUD_PAPER, Dependencies.CLOUD_BRIGADIER, Dependencies.CLOUD_MINECRAFT_EXTRAS
        );
    }

    @Override
    public void onPluginEnable() {
        if (this.successfullyEnabled) {
            logger().severe(" ");
            logger().severe(" ");
            logger().severe(" ");
            logger().severe("Please do not restart plugins at runtime.");
            logger().severe(" ");
            logger().severe(" ");
            logger().severe(" ");
            Bukkit.getPluginManager().disablePlugin(this.javaPlugin);
            return;
        }
        this.successfullyEnabled = true;
        if (!this.successfullyLoaded) {
            logger().severe(" ");
            logger().severe(" ");
            logger().severe(" ");
            logger().severe("Failed to enable CraftEngine. Please check the log on loading stage.");
            logger().severe("To reduce the loss caused by plugin not loaded, now shutting down the server");
            logger().severe(" ");
            logger().severe(" ");
            logger().severe(" ");
            Bukkit.getServer().shutdown();
            return;
        }
        BukkitBlockBehaviors.init();
        BukkitItemBehaviors.init();
        BukkitHitBoxTypes.init();
        PacketConsumers.initEntities(RegistryUtils.currentEntityTypeRegistrySize());
        super.packManager = new BukkitPackManager(this);
        super.senderFactory = new BukkitSenderFactory(this);
        super.recipeManager = new BukkitRecipeManager(this);
        super.commandManager = new BukkitCommandManager(this);
        super.itemBrowserManager = new ItemBrowserManagerImpl(this);
        super.guiManager = new BukkitGuiManager(this);
        super.worldManager = new BukkitWorldManager(this);
        super.soundManager = new BukkitSoundManager(this);
        super.vanillaLootManager = new BukkitVanillaLootManager(this);
        super.fontManager = new BukkitFontManager(this);
        super.advancementManager = new BukkitAdvancementManager(this);
        super.projectileManager = new BukkitProjectileManager(this);
        super.furnitureManager = new BukkitFurnitureManager(this);
        super.onPluginEnable();
        super.compatibilityManager().onEnable();
    }

    @Override
    public void onPluginDisable() {
        super.onPluginDisable();
        if (this.tickTask != null) this.tickTask.cancel();
        if (!Bukkit.getServer().isStopping()) {
            logger().severe(" ");
            logger().severe(" ");
            logger().severe(" ");
            logger().severe("Please do not disable plugins at runtime.");
            logger().severe(" ");
            logger().severe(" ");
            logger().severe(" ");
            Bukkit.getServer().shutdown();
        }
    }

    @Override
    public void platformDelayedEnable() {
        if (Config.metrics()) {
            new Metrics(this.javaPlugin(), 24333);
        }
        // tick task
        if (!VersionHelper.isFolia()) {
            this.tickTask = this.scheduler().sync().runRepeating(() -> {
                for (BukkitServerPlayer serverPlayer : networkManager().onlineUsers()) {
                    serverPlayer.tick();
                }
            }, 1, 1);
        }
    }

    @Override
    public InputStream resourceStream(String filePath) {
        return getResource(CharacterUtils.replaceBackslashWithSlash(filePath));
    }

    private @Nullable InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }
        try {
            URL url = this.getClass().getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public File dataFolderFile() {
        return this.dataFolderPath.toFile();
    }

    @Override
    public Path dataFolderPath() {
        return this.dataFolderPath;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String pluginVersion() {
        return javaPlugin().getDescription().getVersion();
    }

    @Override
    public String serverVersion() {
        return VersionHelper.MINECRAFT_VERSION.version();
    }

    @Override
    public SchedulerAdapter<World> scheduler() {
        return (SchedulerAdapter<World>) scheduler;
    }

    @Override
    public ItemManager<ItemStack> itemManager() {
        return (ItemManager<ItemStack>) itemManager;
    }

    @Override
    public BukkitBlockManager blockManager() {
        return (BukkitBlockManager) blockManager;
    }

    @Override
    public BukkitAdvancementManager advancementManager() {
        return (BukkitAdvancementManager) advancementManager;
    }

    @Override
    public BukkitFurnitureManager furnitureManager() {
        return (BukkitFurnitureManager) furnitureManager;
    }

    @Override
    public SenderFactory<CraftEngine, CommandSender> senderFactory() {
        return (SenderFactory<CraftEngine, CommandSender>) senderFactory;
    }

    public JavaPlugin javaPlugin() {
        return this.javaPlugin;
    }

    public static BukkitCraftEngine instance() {
        return instance;
    }

    @Override
    public BukkitNetworkManager networkManager() {
        return (BukkitNetworkManager) networkManager;
    }

    @Override
    public BukkitPackManager packManager() {
        return (BukkitPackManager) packManager;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void saveResource(String resourcePath) {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        File outFile = new File(dataFolderFile(), resourcePath);
        if (outFile.exists())
            return;

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = resourceStream(resourcePath);
        if (in == null)
            return;

        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataFolderFile(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public BukkitServerPlayer adapt(@NotNull org.bukkit.entity.Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return (BukkitServerPlayer) networkManager().getOnlineUser(player);
    }

    public AntiGriefLib antiGriefProvider() {
        if (this.antiGrief == null) {
            this.antiGrief = AntiGriefLib.builder(this.javaPlugin)
                    .ignoreOP(true)
                    .silentLogs(false)
                    .build();
        }
        return this.antiGrief;
    }
}
