package net.momirealms.craftengine.bukkit.plugin;

import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.craftengine.bukkit.advancement.BukkitAdvancementManager;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehaviors;
import net.momirealms.craftengine.bukkit.block.entity.renderer.element.BukkitBlockEntityElementConfigs;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.element.BukkitFurnitureElementConfigs;
import net.momirealms.craftengine.bukkit.entity.furniture.hitbox.BukkitFurnitureHitboxTypes;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitProjectileManager;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeatManager;
import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.BukkitItemBehaviors;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.loot.BukkitLootManager;
import net.momirealms.craftengine.bukkit.pack.BukkitPackManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitSenderFactory;
import net.momirealms.craftengine.bukkit.plugin.gui.BukkitGuiManager;
import net.momirealms.craftengine.bukkit.plugin.injector.*;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.scheduler.BukkitSchedulerAdapter;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.sound.BukkitSoundManager;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.ReflectionClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unchecked")
public final class BukkitCraftEngine extends CraftEngine {
    private static final String COMPATIBILITY_CLASS = "net.momirealms.craftengine.bukkit.compatibility.BukkitCompatibilityManager";
    private static BukkitCraftEngine instance;
    private SchedulerTask tickTask;
    private boolean successfullyLoaded = false;
    private boolean successfullyEnabled = false;
    private AntiGriefLib antiGrief;
    private JavaPlugin javaPlugin;
    private final Path dataFolderPath;

    BukkitCraftEngine(JavaPlugin plugin) {
        this(new JavaPluginLogger(plugin.getLogger()), plugin.getDataFolder().toPath().toAbsolutePath(),
                new ReflectionClassPathAppender(plugin.getClass().getClassLoader()), new ReflectionClassPathAppender(plugin.getClass().getClassLoader()));
        this.setJavaPlugin(plugin);
    }

    BukkitCraftEngine(PluginLogger logger, Path dataFolderPath, ClassPathAppender sharedClassPathAppender, ClassPathAppender privateClassPathAppender) {
        super((p) -> {
            CraftEngineReloadEvent event = new CraftEngineReloadEvent((BukkitCraftEngine) p);
            EventUtils.fireAndForget(event);
        });
        instance = this;
        this.dataFolderPath = dataFolderPath;
        super.sharedClassPathAppender = sharedClassPathAppender;
        super.privateClassPathAppender = privateClassPathAppender;
        super.logger = logger;
        super.platform = new BukkitPlatform(this);
        super.scheduler = new BukkitSchedulerAdapter(this);
        Class<?> compatibilityClass = ReflectionUtils.getClazz(COMPATIBILITY_CLASS);
        if (compatibilityClass != null) {
            try {
                super.compatibilityManager = (CompatibilityManager) Objects.requireNonNull(ReflectionUtils.getConstructor(compatibilityClass, 0)).newInstance(this);
            } catch (ReflectiveOperationException e) {
                logger().warn("Compatibility class could not be instantiated: " + compatibilityClass.getName());
            }
        }
    }

    void setJavaPlugin(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @Override
    public void setUpConfigAndLocale() {
        super.setUpConfigAndLocale();
        super.packManager = new BukkitPackManager(this);
    }

    // 这个方法应该尽早被执行，最好是boostrap阶段
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
        try {
            FeatureInjector.init();
        } catch (Exception e) {
            throw new InjectionException("Error injecting features", e);
        }
        try {
            BlockStateProviderInjector.init();
        } catch (Exception e) {
            throw new InjectionException("Error injecting block state providers", e);
        }
    }

    @Override
    public void onPluginLoad() {
        // 普通bukkit插件会到这里才注册自定义方块
        if (super.blockManager == null) {
            this.injectRegistries();
        }
        // 注入一些新的类型，但是并不需要太早
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
        // 初始化一些注册表
        super.onPluginLoad();
        BukkitBlockBehaviors.init();
        BukkitItemBehaviors.init();
        BukkitFurnitureHitboxTypes.init();
        BukkitBlockEntityElementConfigs.init();
        BukkitFurnitureElementConfigs.init();
        // 初始化 onload 阶段的兼容性
        super.compatibilityManager().onLoad();
        // 创建网络管理器
        super.networkManager = new BukkitNetworkManager(this);
        // 初始化方块管理器，获取镜像注册表，初始化网络映射
        super.blockManager.init();
        // 初始化物品管理器
        super.itemManager = new BukkitItemManager(this);
        // 初始化配方管理器
        super.recipeManager = new BukkitRecipeManager(this);
        // 初始化GUI管理器
        super.guiManager = new BukkitGuiManager(this);
        // 初始化世界管理器
        super.worldManager = new BukkitWorldManager(this);
        // 初始化声音管理器
        super.soundManager = new BukkitSoundManager(this);
        // 初始化战利品管理器
        super.lootManager = new BukkitLootManager(this);
        // 初始化字体管理器
        super.fontManager = new BukkitFontManager(this);
        // 初始化进度管理器
        super.advancementManager = new BukkitAdvancementManager(this);
        // 初始化弹射物管理器
        super.projectileManager = new BukkitProjectileManager(this);
        // 初始化座椅管理器
        super.seatManager = new BukkitSeatManager(this);
        // 初始化家具管理器
        super.furnitureManager = new BukkitFurnitureManager(this);
        // 初始化队伍管理器
        super.teamManager = new BukkitTeamManager(this);
        // 初始化虚拟队伍
        super.teamManager.init();
        // 注册默认的parser
        this.registerDefaultParsers();
        // 完成加载
        this.successfullyLoaded = true;
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
        // 初始化指令发送者工厂
        super.senderFactory = new BukkitSenderFactory(this);
        // 初始化指令管理器
        super.commandManager = new BukkitCommandManager(this);
        try {
            super.compatibilityManager().onEnable();
        } catch (Throwable t) {
            this.logger.severe("Failed to enable compatibility manager", t);
        }
        super.onPluginEnable();
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

    @Override
    public BukkitFontManager fontManager() {
        return (BukkitFontManager) fontManager;
    }

    @Override
    public BukkitWorldManager worldManager() {
        return (BukkitWorldManager) worldManager;
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

    @Nullable
    public BukkitServerPlayer adapt(@Nullable Player player) {
        if (player == null) return null;
        return (BukkitServerPlayer) networkManager().getOnlineUser(player);
    }

    public AntiGriefLib antiGriefProvider() {
        if (this.antiGrief == null) {
            this.antiGrief = AntiGriefLib.builder(this.javaPlugin)
                    .ignoreOP(true)
                    .silentLogs(false)
                    .bypassPermission("craftengine.antigrief.bypass")
                    .build();
        }
        return this.antiGrief;
    }
}
