package net.momirealms.craftengine.bukkit.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.momirealms.craftengine.bukkit.plugin.agent.RuntimePatcher;
import net.momirealms.craftengine.bukkit.plugin.classpath.BukkitClassPathAppender;
import net.momirealms.craftengine.bukkit.plugin.classpath.PaperPluginClassPathAppender;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.logger.Slf4jPluginLogger;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.chunk.storage.StorageType;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class PaperCraftEngineBootstrap implements PluginBootstrap {
    private static final Class<?> clazz$PluginProviderContext = PluginProviderContext.class;
    private static final Class<?> clazz$ComponentLogger = Objects.requireNonNull(
            ReflectionUtils.getClazz(
                    "net{}kyori{}adventure{}text{}logger{}slf4j{}ComponentLogger".replace("{}", ".")
            )
    );
    private static final Method method$PluginProviderContext$getLogger = Objects.requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$PluginProviderContext, clazz$ComponentLogger, new String[]{"getLogger"}
            )
    );

    BukkitCraftEngine plugin;

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        PluginLogger logger;
        try {
            logger = new Slf4jPluginLogger((org.slf4j.Logger) method$PluginProviderContext$getLogger.invoke(context));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to getLogger", e);
        }
        this.plugin = new BukkitCraftEngine(
                logger,
                context.getDataDirectory(),
                new BukkitClassPathAppender(),
                new PaperPluginClassPathAppender(this.getClass().getClassLoader())
        );
        this.plugin.applyDependencies();
        this.plugin.setupProxy();
        this.plugin.setUpConfigAndLocale();
        if (isDatapackDiscoveryAvailable()) {
            new ModernEventHandler(context, this.plugin).register();
        } else {
            try {
                logger.info("Patching the server...");
                RuntimePatcher.patch(this.plugin);
            } catch (Exception e) {
                throw new RuntimeException("Failed to patch server", e);
            }
        }
        this.backupWorldData(logger, context);
    }

    private static final Set<String> NON_WORLD_FOLDERS = Set.of("cache", "config", "libraries", "logs", "plugins", "versions");

    private void backupWorldData(PluginLogger logger, BootstrapContext context) {
        if (VersionHelper.isOrAbove26_1) {
            boolean first = true;
            JsonObject worldDataToMigrate = new JsonObject();
            Path rootPath = context.getDataDirectory().toAbsolutePath().getParent().getParent();
            try {
                if (rootPath != null && Files.exists(rootPath)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath, Files::isDirectory)) {
                        for (Path folder : stream) {
                            if (NON_WORLD_FOLDERS.contains(folder.getFileName().toString())) continue;

                            // 不是世界文件夹
                            Path levelDatFile = folder.resolve("level.dat");
                            if (!Files.exists(levelDatFile)) {
                                continue;
                            }

                            // 用户自定义plugins文件夹，避免备份ce插件文件夹
                            Path customDataFolder = folder.resolve("craftengine");
                            if (!Files.exists(customDataFolder) || !Files.isDirectory(customDataFolder) || Files.exists(customDataFolder.resolve("config.yml"))) {
                                continue;
                            }

                            // 创建备份文件夹
                            String worldName = folder.getFileName().toString();
                            Path backupFolder = context.getDataDirectory().resolve("world_upgrade_backup");
                            if (!Files.exists(backupFolder)) {
                                Files.createDirectories(backupFolder);
                            }

                            if (first) {
                                first = false;
                                logger.warn("=====================================================");
                                logger.warn("CraftEngine storage migration is required during startup.");
                                logger.warn("The plugin will automatically backup and migrate your data.");
                                logger.warn("");
                                logger.warn("If you do not have enough space or your server's world ");
                                logger.warn("storage is less than standard, please immediately press ");
                                logger.warn("Ctrl+C or use other methods to kill this process and do manual migration.");
                                logger.warn("");
                                logger.warn("To perform a manual migration, move the 'craftengine' folder");
                                logger.warn("from the world folder to the new world folder.");
                                logger.warn("=====================================================");
                                logger.warn("");
                                logger.warn("The automatic migration will start in 30 seconds.");
                                logger.warn("");
                                logger.warn("=====================================================");
                                try {
                                    Thread.sleep(Duration.ofSeconds(30).toMillis());
                                } catch (InterruptedException ignored) {
                                }
                            }

                            // 创建备份文件夹
                            Path backupFile = backupFolder.resolve(worldName + ".zip");
                            logger.warn("Legacy world format detected: '" + worldName + "'. Starting backup...");
                            Timestamp timestamp = new Timestamp();
                            ZipUtils.compress(customDataFolder, backupFile);
                            logger.warn("Backup completed for '" + worldName + "' in " + String.format("%.2f", timestamp.deltaMillis() / 1000d) + "s. Saved to: " + backupFile.toAbsolutePath());
                            FileUtils.deleteDirectory(customDataFolder);
                            logger.warn("Deleted outdated region files for '" + worldName + "' in " + String.format("%.2f", timestamp.deltaMillis() / 1000d) + "s");

                            // 只有MCA支持自动迁移
                            if (Config.chunkStorageType() == StorageType.MCA) {
                                try {
                                    CompoundTag datCompound = NBTUtils.readCompressed(Files.newInputStream(levelDatFile));
                                    if (datCompound != null) {
                                        CompoundTag dataCompound = datCompound.getCompound("Data");
                                        if (dataCompound != null) {
                                            CompoundTag spawnCompound = dataCompound.getCompound("spawn");
                                            if (spawnCompound != null) {
                                                String dimension = spawnCompound.getString("dimension");
                                                if (dimension != null) {
                                                    worldDataToMigrate.addProperty(worldName, dimension);
                                                }
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    logger.error("Failed to read level.dat file", e);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to backup worlds", e);
            }

            try {
                if (!worldDataToMigrate.isEmpty()) {
                    Path tempData = context.getDataDirectory().resolve("worlds_to_migrate.json");
                    if (Files.exists(tempData)) {
                        JsonObject existingData = GsonHelper.readJsonObjectFromFile(tempData);
                        if (existingData != null) {
                            for (Map.Entry<String, JsonElement> entry : existingData.entrySet()) {
                                if (!worldDataToMigrate.has(entry.getKey())) {
                                    worldDataToMigrate.add(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                    }
                    GsonHelper.writeJsonFile(worldDataToMigrate, tempData);
                }
            } catch (IOException e) {
                logger.error("Failed to save worlds to migrate", e);
            }
        }
    }

    private static boolean isDatapackDiscoveryAvailable() {
        try {
            Class<?> eventsClass = Class.forName("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents");
            eventsClass.getField("DATAPACK_DISCOVERY");
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            return false;
        }
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new PaperCraftEnginePlugin(this);
    }

    public static class ModernEventHandler {
        private final BootstrapContext context;
        private final BukkitCraftEngine plugin;

        public ModernEventHandler(BootstrapContext context, BukkitCraftEngine plugin) {
            this.context = context;
            this.plugin = plugin;
        }

        public void register() {
            this.context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, (e) -> {
                try {
                    this.plugin.injectRegistries();
                } catch (Throwable ex) {
                    this.plugin.logger().warn("Failed to inject registries", ex);
                }
            });
        }
    }
}
