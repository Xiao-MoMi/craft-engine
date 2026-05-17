package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.world.BukkitStorageAdaptor;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.bukkit.world.chunk.storage.PersistentDataContainerStorage;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.storage.DefaultRegionFileStorage;
import net.momirealms.craftengine.core.world.chunk.storage.StorageType;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MigrateWorldStorageCommand extends BukkitCommandFeature<CommandSender> {
    private static final Pattern MCA_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");
    private static volatile boolean migrationInProgress = false;

    private static final int CHUNKS_PER_REGION = 32;
    private static final int PROGRESS_INTERVAL = 100;

    public MigrateWorldStorageCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder.required("world", WorldParser.worldParser())
                .required("type", EnumParser.enumComponent(StorageType.class)
                        .suggestionProvider(new SuggestionProvider<>() {
                            @Override
                            public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext context, @NonNull CommandInput input) {
                                List<Suggestion> suggestions = new ArrayList<>(1);
                                switch (Config.chunkStorageType()) {
                                    case MCA -> suggestions.add(Suggestion.suggestion("pdc"));
                                    case PDC -> suggestions.add(Suggestion.suggestion("mca"));
                                }
                                return CompletableFuture.completedFuture(suggestions);
                            }
                        }))
                .handler(context -> {
                    // 检查是否已有迁移任务在进行
                    if (migrationInProgress) {
                        context.sender().sendMessage("Another migration is already in progress. Please wait until it finishes.");
                        return;
                    }

                    // 设置迁移标志，保证单任务
                    migrationInProgress = true;

                    CommandSender sender = context.sender();
                    World world = context.get("world");
                    StorageType sourceType = context.get("type");
                    StorageType targetType = Config.chunkStorageType();

                    if (sourceType != StorageType.MCA && sourceType != StorageType.PDC) {
                        sender.sendMessage("You can only migrate worlds from MCA or PDC.");
                        migrationInProgress = false;
                        return;
                    }

                    if (sourceType == targetType) {
                        sender.sendMessage("Source storage type is the same as current target storage type: " + sourceType + ".");
                        sender.sendMessage("Please switch config to the target storage type first, then run migration from the old type.");
                        migrationInProgress = false;
                        return;
                    }

                    Path regionFolder = getRegionFolder(world, sourceType);
                    if (!Files.isDirectory(regionFolder)) {
                        sender.sendMessage("Cannot find region folder: " + regionFolder);
                        migrationInProgress = false;
                        return;
                    }

                    List<RegionPos> regions = getRegionPositions(regionFolder);
                    if (regions.isEmpty()) {
                        sender.sendMessage("Cannot find any region file in: " + regionFolder);
                        migrationInProgress = false;
                        return;
                    }

                    BukkitWorld adaptedWorld = BukkitAdaptor.adapt(world);
                    WorldDataStorage targetStorage = adaptedWorld.storageWorld().worldDataStorage();
                    WorldDataStorage sourceStorage = createSourceStorage(sourceType, world, adaptedWorld);

                    List<ChunkPos> chunksToProcess = buildChunkPositions(regions);
                    MigrationStats stats = new MigrationStats(chunksToProcess.size());

                    ExecutorService executor = newMigrationExecutor(world.getName());

                    sender.sendMessage("Starting storage migration for world: " + world.getName() +
                            ". Source: " + sourceType + ", target: " + targetType +
                            ", regions: " + regions.size() +
                            ", chunks to check: " + chunksToProcess.size());

                    migrateChunksSequentially(sender, chunksToProcess, 0, world, targetStorage, sourceStorage, stats, executor);
                });
    }

    private Path getRegionFolder(World world, StorageType sourceType) {
        return switch (sourceType) {
            case MCA -> world.getWorldPath().resolve("craftengine");
            case PDC -> world.getWorldPath().resolve("region");
            default -> throw new IllegalArgumentException("Unsupported source storage type: " + sourceType);
        };
    }

    private WorldDataStorage createSourceStorage(StorageType sourceType, World world, BukkitWorld adaptedWorld) {
        return switch (sourceType) {
            case MCA -> new DefaultRegionFileStorage(world.getWorldPath().resolve("craftengine"),
                    VersionHelper.isFolia ? BukkitStorageAdaptor.FOLIA_FACTORY : BukkitStorageAdaptor.BUKKIT_FACTORY);
            case PDC -> new PersistentDataContainerStorage(adaptedWorld,
                    VersionHelper.isFolia ? BukkitStorageAdaptor.FOLIA_FACTORY : BukkitStorageAdaptor.BUKKIT_FACTORY);
            default -> throw new IllegalArgumentException("Unsupported source storage type: " + sourceType);
        };
    }

    private List<ChunkPos> buildChunkPositions(List<RegionPos> regions) {
        List<ChunkPos> chunks = new ArrayList<>(regions.size() * CHUNKS_PER_REGION * CHUNKS_PER_REGION);

        for (RegionPos region : regions) {
            int baseX = region.x() * CHUNKS_PER_REGION;
            int baseZ = region.z() * CHUNKS_PER_REGION;

            for (int x = 0; x < CHUNKS_PER_REGION; x++) {
                for (int z = 0; z < CHUNKS_PER_REGION; z++) {
                    chunks.add(new ChunkPos(baseX + x, baseZ + z));
                }
            }
        }

        return chunks;
    }

    private ExecutorService newMigrationExecutor(String worldName) {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "craftengine-storage-migration-" + worldName);
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    private void migrateChunksSequentially(CommandSender sender, List<ChunkPos> chunks, int index, World world,
                                           WorldDataStorage targetStorage, WorldDataStorage sourceStorage,
                                           MigrationStats stats, ExecutorService executor) {
        if (index >= chunks.size()) {
            finishMigration(sender, world, sourceStorage, stats, executor);
            return;
        }

        ChunkPos pos = chunks.get(index);

        world.getChunkAtAsync(pos.x(), pos.z(), false).thenAcceptAsync(chunk -> {
            stats.checked.incrementAndGet();

            if (chunk == null) {
                stats.skipped.incrementAndGet();
                return;
            }

            try {
                CompoundTag tag = sourceStorage.readChunkTagAt(pos);
                if (tag == null) {
                    stats.empty.incrementAndGet();
                    return;
                }

                targetStorage.writeChunkTagAt(pos, tag);
                stats.migrated.incrementAndGet();
            } catch (Exception e) {
                stats.failed.incrementAndGet();
                this.plugin().logger().warn("Failed to migrate data for chunk at [" + pos.x() + ", " + pos.z() + "]", e);
            }
        }, executor).exceptionally(ex -> {
            stats.checked.incrementAndGet();
            stats.failed.incrementAndGet();
            this.plugin().logger().error("An unexpected error occurred while loading chunk at [" + pos.x() + ", " + pos.z() + "]", ex);
            return null;
        }).whenCompleteAsync((ignored, ex) -> {
            int checked = stats.checked.get();

            if (checked > 0 && checked % PROGRESS_INTERVAL == 0) {
                this.plugin().logger().info("Storage migration progress for world: " + world.getName() +
                        ". Checked: " + checked + "/" + stats.total +
                        ", migrated: " + stats.migrated.get() +
                        ", empty: " + stats.empty.get() +
                        ", skipped: " + stats.skipped.get() +
                        ", failed: " + stats.failed.get());
            }

            migrateChunksSequentially(sender, chunks, index + 1, world, targetStorage, sourceStorage, stats, executor);
        }, executor);
    }

    private void finishMigration(CommandSender sender, World world, WorldDataStorage sourceStorage, MigrationStats stats, ExecutorService executor) {
        try {
            if (sourceStorage instanceof AutoCloseable closeable) {
                closeable.close();
            }
        } catch (Exception e) {
            this.plugin().logger().warn("Failed to safely close source storage.", e);
        } finally {
            executor.shutdown();
            migrationInProgress = false; // ✅ 重置标志
        }

        String message = "Storage migration finished for world: " + world.getName() +
                ". Checked: " + stats.checked.get() + "/" + stats.total +
                ", migrated: " + stats.migrated.get() +
                ", empty: " + stats.empty.get() +
                ", skipped: " + stats.skipped.get() +
                ", failed: " + stats.failed.get();

        this.plugin().logger().info(message);
        sender.sendMessage(message);
    }

    @Override
    public String getFeatureID() {
        return "migrate_world_storage";
    }

    private List<RegionPos> getRegionPositions(Path regionFolder) {
        List<RegionPos> positions = new ArrayList<>(128);

        if (!Files.isDirectory(regionFolder)) return positions;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionFolder, "r.*.*.mca")) {
            for (Path entry : stream) {
                if (!Files.isRegularFile(entry)) continue;

                Matcher matcher = MCA_PATTERN.matcher(entry.getFileName().toString());
                if (!matcher.matches()) continue;

                try {
                    int x = Integer.parseInt(matcher.group(1));
                    int z = Integer.parseInt(matcher.group(2));
                    positions.add(new RegionPos(x, z));
                } catch (NumberFormatException e) {
                    this.plugin().logger().warn("Invalid region file name: " + entry.getFileName(), e);
                }
            }
        } catch (IOException e) {
            this.plugin().logger().warn("Failed to read region folder: " + regionFolder, e);
        }

        positions.sort(Comparator.comparingInt(RegionPos::x).thenComparingInt(RegionPos::z));
        return positions;
    }

    private static final class MigrationStats {
        private final int total;
        private final AtomicInteger checked = new AtomicInteger();
        private final AtomicInteger migrated = new AtomicInteger();
        private final AtomicInteger empty = new AtomicInteger();
        private final AtomicInteger skipped = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();

        private MigrationStats(int total) { this.total = total; }
    }

    private record RegionPos(int x, int z) {}
}