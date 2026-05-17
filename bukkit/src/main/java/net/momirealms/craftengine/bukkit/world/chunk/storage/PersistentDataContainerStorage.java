package net.momirealms.craftengine.bukkit.world.chunk.storage;

import net.momirealms.craftengine.bukkit.world.chunk.BukkitChunkAccess;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.craftengine.core.world.chunk.serialization.DefaultChunkSerializer;
import net.momirealms.craftengine.core.world.chunk.storage.ChunkFactory;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Objects;

public class PersistentDataContainerStorage implements WorldDataStorage {
    private static final NamespacedKey CHUNK_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:chunk_data"));
    private final ChunkFactory chunkFactory;
    private final World world;

    public PersistentDataContainerStorage(World world, ChunkFactory chunkFactory) {
        this.chunkFactory = chunkFactory;
        this.world = world;
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) throws IOException {
        if (chunkAccess == null) {
            return this.chunkFactory.create(world, pos);
        }
        BukkitChunkAccess access = (BukkitChunkAccess) chunkAccess;
        PersistentDataContainer pdc = access.getPersistentDataContainer();
        byte[] bytes = pdc.get(CHUNK_KEY, PersistentDataType.BYTE_ARRAY);
        if (bytes == null) {
            return this.chunkFactory.create(world, pos);
        }
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            CompoundTag tag = NBT.readCompound(dis, false);
            return DefaultChunkSerializer.deserialize(this.chunkFactory, world, pos, tag);
        }
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) throws IOException {
        BukkitChunkAccess chunkAccess = (BukkitChunkAccess) chunk.chunkAccess();
        if (chunkAccess == null) {
            return;
        }
        CompoundTag nbt = DefaultChunkSerializer.serialize(chunk);
        PersistentDataContainer pdc = chunkAccess.getPersistentDataContainer();
        setPdc(pdc, nbt);
    }

    @Override
    public @Nullable CompoundTag readChunkTagAt(@NotNull ChunkPos pos) throws IOException {
        org.bukkit.World bukkitWorld = (org.bukkit.World) this.world.platformWorld();
        PersistentDataContainer pdc = bukkitWorld.getChunkAt(pos.x, pos.z).getPersistentDataContainer();
        byte[] bytes = pdc.get(CHUNK_KEY, PersistentDataType.BYTE_ARRAY);
        if (bytes == null) {
            return null;
        }
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return NBT.readCompound(dis, false);
        }
    }

    @Override
    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) throws IOException {
        org.bukkit.World bukkitWorld = (org.bukkit.World) this.world.platformWorld();
        PersistentDataContainer pdc = bukkitWorld.getChunkAt(pos.x, pos.z).getPersistentDataContainer();
        setPdc(pdc, nbt);
    }

    private void setPdc(@NotNull PersistentDataContainer pdc, @Nullable CompoundTag nbt) throws IOException {
        if (nbt == null) {
            pdc.remove(CHUNK_KEY);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(bos)) {
                NBT.writeCompound(nbt, dos, false);
            }
            pdc.set(CHUNK_KEY, PersistentDataType.BYTE_ARRAY, bos.toByteArray());
        }
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) {
        // pdc doesn't need this as the data is cleared together with vanilla chunks
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws IOException {
    }
}
