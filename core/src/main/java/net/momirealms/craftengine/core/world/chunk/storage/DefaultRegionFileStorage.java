package net.momirealms.craftengine.core.world.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ExceptionCollector;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.craftengine.core.world.chunk.serialization.DefaultChunkSerializer;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultRegionFileStorage implements WorldDataStorage {
    private final Path folder;

    public static final String REGION_FILE_SUFFIX = ".mca";
    public static final String REGION_FILE_PREFIX = "r.";
    public static final int MAX_NON_EXISTING_CACHE = 4096;

    public final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap<>();
    private final LongLinkedOpenHashSet nonExistingRegionFiles = new LongLinkedOpenHashSet();
    private final ChunkFactory chunkFactory;

    public DefaultRegionFileStorage(Path directory, ChunkFactory chunkFactory) {
        this.folder = directory;
        this.chunkFactory = chunkFactory;
    }

    private boolean doesRegionFilePossiblyExist(long position) {
        synchronized (this.nonExistingRegionFiles) {
            if (this.nonExistingRegionFiles.contains(position)) {
                this.nonExistingRegionFiles.addAndMoveToFirst(position);
                return false;
            }
            return true;
        }
    }

    private void createRegionFile(long position) {
        synchronized (this.nonExistingRegionFiles) {
            this.nonExistingRegionFiles.remove(position);
        }
    }

    private void markNonExisting(long position) {
        synchronized (this.nonExistingRegionFiles) {
            if (this.nonExistingRegionFiles.addAndMoveToFirst(position)) {
                while (this.nonExistingRegionFiles.size() >= MAX_NON_EXISTING_CACHE) {
                    this.nonExistingRegionFiles.removeLastLong();
                }
            }
        }
    }

    public boolean doesRegionFileNotExistNoIO(ChunkPos pos) {
        long key = ChunkPos.asLong(pos.regionX(), pos.regionZ());
        return !this.doesRegionFilePossiblyExist(key);
    }

    public final synchronized RegionFile getRegionFileIfLoaded(ChunkPos pos) {
        return this.regionCache.getAndMoveToFirst(ChunkPos.asLong(pos.regionX(), pos.regionZ()));
    }

    public boolean chunkExists(ChunkPos pos) throws IOException {
        RegionFile regionfile = getRegionFile(pos, true);
        return regionfile != null && regionfile.hasChunk(pos);
    }

    public synchronized RegionFile getRegionFile(ChunkPos pos, boolean existingOnly) throws IOException {
        long key = ChunkPos.asLong(pos.regionX(), pos.regionZ());
        RegionFile ret = this.regionCache.getAndMoveToFirst(key);
        if (ret != null) {
            return ret;
        } else if (existingOnly && !this.doesRegionFilePossiblyExist(key)) {
            return null;
        } else {
            if (this.regionCache.size() >= 256) {
                this.regionCache.removeLast().close();
            }

            Path path = this.folder.resolve(REGION_FILE_PREFIX + pos.regionX() + "." + pos.regionZ() + REGION_FILE_SUFFIX);
            if (existingOnly && !Files.exists(path)) {
                this.markNonExisting(key);
                return null;
            } else {
                this.createRegionFile(key);
                FileUtils.createDirectoriesSafe(this.folder);
                ret = new RegionFile(path, this.folder, CompressionMethod.fromId(Config.compressionMethod()));
                this.regionCache.putAndMoveToFirst(key, ret);
                return ret;
            }
        }
    }

    public static ChunkPos getRegionFileCoordinates(Path file) {
        String fileName = file.getFileName().toString();
        if (!fileName.startsWith(REGION_FILE_PREFIX) || !fileName.endsWith(REGION_FILE_SUFFIX)) {
            return null;
        }
        String[] split = fileName.split("\\.");
        if (split.length != 4) {
            return null;
        }
        try {
            int x = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            return new ChunkPos(x << 5, z << 5);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public CEChunk readNewChunkAt(CEWorld world, ChunkPos pos) throws IOException {
        RegionFile regionFile = this.getRegionFile(pos, false);
        synchronized (regionFile) {
            if (regionFile.hasChunk(pos)) {
                regionFile.clear(pos);
            }
            return this.chunkFactory.create(world, pos);
        }
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) throws IOException {
        RegionFile regionFile = this.getRegionFile(pos, true);
        if (regionFile == null) {
            return this.chunkFactory.create(world, pos);
        }

        synchronized (regionFile) {
            try (DataInputStream dataInputStream = regionFile.getChunkDataInputStream(pos)) {
                if (dataInputStream == null) {
                    return this.chunkFactory.create(world, pos);
                }
                CompoundTag tag = NBT.readCompound(dataInputStream, false);
                return DefaultChunkSerializer.deserialize(this.chunkFactory, world, pos, tag);
            }
        }
    }

    @Override
    public CompoundTag readChunkTagAt(@NotNull ChunkPos pos) throws IOException {
        RegionFile regionFile = this.getRegionFile(pos, true);
        if (regionFile == null) {
            return null;
        }
        synchronized (regionFile) {
            try (DataInputStream dataInputStream = regionFile.getChunkDataInputStream(pos)) {
                if (dataInputStream == null) {
                    return null;
                }
                return NBT.readCompound(dataInputStream, false);
            }
        }
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) throws IOException {
        CompoundTag nbt = DefaultChunkSerializer.serialize(chunk);
        writeChunkTagAt(pos, nbt);
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) throws IOException {
        this.writeChunkTagAt(pos, null);
    }

    @Override
    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) throws IOException {
        RegionFile regionFile = this.getRegionFile(pos, nbt == null);
        if (regionFile == null) return;

        synchronized (regionFile) {
            if (nbt == null) {
                regionFile.clear(pos);
            } else {
                try (DataOutputStream dataOutputStream = regionFile.getChunkDataOutputStream(pos)) {
                    NBT.writeCompound(nbt, dataOutputStream, false);
                }
            }
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        ExceptionCollector<IOException> collector = new ExceptionCollector<>(IOException.class);
        for (RegionFile regionFile : this.regionCache.values()) {
            try {
                regionFile.flush();
            } catch (IOException e) {
                collector.add(e);
            }
        }
        collector.throwIfPresent();
    }

    @Override
    public synchronized void close() throws IOException {
        ExceptionCollector<IOException> collector = new ExceptionCollector<>(IOException.class);
        for (RegionFile regionfile : this.regionCache.values()) {
            try {
                regionfile.close();
            } catch (IOException ioexception) {
                collector.add(ioexception);
            }
        }
        collector.throwIfPresent();
    }
}