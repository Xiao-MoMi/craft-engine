package net.momirealms.craftengine.core.world.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ExceptionCollector;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
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

public final class DefaultRegionFileStorage implements WorldDataStorage {
    private final Path folder;

    public static final String REGION_FILE_SUFFIX = ".mca";
    public static final String REGION_FILE_PREFIX = "r.";
    public static final int MAX_NON_EXISTING_CACHE = 1024 * 64;

    public final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap<>();
    private final LongLinkedOpenHashSet nonExistingRegionFiles = new LongLinkedOpenHashSet();

    public DefaultRegionFileStorage(Path directory) {
        this.folder = directory;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private synchronized boolean doesRegionFilePossiblyExist(long position) {
        if (this.nonExistingRegionFiles.contains(position)) {
            this.nonExistingRegionFiles.addAndMoveToFirst(position);
            return false;
        }
        return true;
    }

    private synchronized void createRegionFile(long position) {
        this.nonExistingRegionFiles.remove(position);
    }

    private synchronized void markNonExisting(long position) {
        if (this.nonExistingRegionFiles.addAndMoveToFirst(position)) {
            while (this.nonExistingRegionFiles.size() >= MAX_NON_EXISTING_CACHE) {
                this.nonExistingRegionFiles.removeLastLong();
            }
        }
    }

    public synchronized boolean doesRegionFileNotExistNoIO(ChunkPos pos) {
        long key = ChunkPos.asLong(pos.regionX(), pos.regionZ());
        return !this.doesRegionFilePossiblyExist(key);
    }

    public synchronized RegionFile getRegionFileIfLoaded(ChunkPos pos) {
        return this.regionCache.getAndMoveToFirst(ChunkPos.asLong(pos.regionX(), pos.regionZ()));
    }

    public synchronized boolean chunkExists(ChunkPos pos) throws IOException {
        RegionFile regionfile = getRegionFile(pos, true, false);
        return regionfile != null && regionfile.hasChunk(pos);
    }

    public synchronized RegionFile getRegionFile(ChunkPos pos, boolean existingOnly, boolean lock) throws IOException {
        long chunkPosLongKey = ChunkPos.asLong(pos.regionX(), pos.regionZ());
        RegionFile regionfile = this.regionCache.getAndMoveToFirst(chunkPosLongKey);
        if (regionfile != null) {
            if (lock) {
                regionfile.fileLock.lock();
            }
            return regionfile;
        } else {
            if (existingOnly && !this.doesRegionFilePossiblyExist(chunkPosLongKey)) {
                return null;
            }
            if (this.regionCache.size() >= 256) {
                this.regionCache.removeLast().close();
            }
            Path path = this.folder.resolve(REGION_FILE_PREFIX + pos.regionX() + "." + pos.regionZ() + REGION_FILE_SUFFIX);
            if (existingOnly && !Files.exists(path)) {
                this.markNonExisting(chunkPosLongKey);
                return null;
            } else {
                this.createRegionFile(chunkPosLongKey);
            }
            FileUtils.createDirectoriesSafe(this.folder);
            RegionFile newRegionFile = new RegionFile(path, this.folder, CompressionMethod.fromId(Config.compressionMethod()));

            this.regionCache.putAndMoveToFirst(chunkPosLongKey, newRegionFile);
            if (lock) {
                newRegionFile.fileLock.lock();
            }
            return newRegionFile;
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
        RegionFile regionFile = this.getRegionFile(pos, false, true);
        try {
            if (regionFile.doesChunkExist(pos)) {
                regionFile.clear(pos);
            }
            return new CEChunk(world, pos);
        } finally {
            regionFile.fileLock.unlock();
        }
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos) throws IOException {
        RegionFile regionFile = this.getRegionFile(pos, false, true);
        try {
            DataInputStream dataInputStream = regionFile.getChunkDataInputStream(pos);
            CompoundTag tag;
            try {
                if (dataInputStream == null) {
                    return new CEChunk(world, pos);
                }
                tag = NBT.readCompound(dataInputStream, false);
            } catch (Throwable t1) {
                try {
                    dataInputStream.close();
                } catch (Throwable t2) {
                    t1.addSuppressed(t2);
                }
                throw t1;
            }
            dataInputStream.close();
            return DefaultChunkSerializer.deserialize(world, pos, tag);
        } finally {
            regionFile.fileLock.unlock();
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

    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) throws IOException {
        RegionFile regionFile = this.getRegionFile(pos, nbt == null, true);
        try {
            if (nbt == null) {
                regionFile.clear(pos);
            } else {
                DataOutputStream dataOutputStream = regionFile.getChunkDataOutputStream(pos);
                try {
                    NBT.writeCompound(nbt, dataOutputStream, false);
                } catch (Throwable t1) {
                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (Throwable t2) {
                            t1.addSuppressed(t2);
                        }
                    }
                    throw t1;
                }
                dataOutputStream.close();
            }
        } finally {
            regionFile.fileLock.unlock();
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        for (RegionFile regionFile : this.regionCache.values()) {
            regionFile.flush();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        ExceptionCollector<IOException> collector = new ExceptionCollector<>();
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
