package net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld;

import com.flowpowered.nbt.ByteArrayTag;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import net.momirealms.craftengine.bukkit.world.BukkitStorageAdaptor;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitCEChunk;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.craftengine.core.world.chunk.serialization.DefaultChunkSerializer;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Optional;

public final class LegacySlimeWorldDataStorage implements WorldDataStorage {
    private final WeakReference<com.infernalsuite.aswm.api.world.SlimeWorld> slimeWorld;

    public LegacySlimeWorldDataStorage(SlimeWorld slimeWorld) {
        this.slimeWorld = new WeakReference<>(slimeWorld);
    }

    public SlimeWorld getWorld() {
        return slimeWorld.get();
    }

    @Override
    public CEChunk readNewChunkAt(CEWorld world, ChunkPos pos) {
        return readChunkAt(world, pos, null);
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return new BukkitCEChunk(world, pos);
        Optional<ByteArrayTag> tag = slimeChunk.getExtraData().getAsByteArrayTag("craftengine");
        if (tag.isEmpty()) return new BukkitCEChunk(world, pos);
        try {
            CompoundTag compoundTag = NBT.fromBytes(tag.get().getValue());
            if (compoundTag == null) return new BukkitCEChunk(world, pos);
            return DefaultChunkSerializer.deserialize(BukkitStorageAdaptor.BUKKIT_FACTORY, world, pos, compoundTag);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read chunk tag from slime world. " + pos, e);
        }
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        writeChunkTagAt(pos, DefaultChunkSerializer.serialize(chunk));
    }

    @Override
    public @Nullable CompoundTag readChunkTagAt(@NotNull ChunkPos pos) throws IOException {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return null;
        Optional<ByteArrayTag> tag = slimeChunk.getExtraData().getAsByteArrayTag("craftengine");
        if (tag.isEmpty()) return null;
        return NBT.fromBytes(tag.get().getValue());
    }

    @Override
    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        if (nbt == null) {
            slimeChunk.getExtraData().getValue().remove("craftengine");
        } else {
            try {
                slimeChunk.getExtraData().getValue().put("craftengine", new ByteArrayTag("craftengine", NBT.toBytes(nbt)));
            } catch (Exception e) {
                throw new RuntimeException("Failed to write chunk tag to slime world. " + pos, e);
            }
        }
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        slimeChunk.getExtraData().getValue().remove("craftengine");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
