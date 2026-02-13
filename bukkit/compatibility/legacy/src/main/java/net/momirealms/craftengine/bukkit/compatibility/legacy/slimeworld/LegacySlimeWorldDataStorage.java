package net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld;

import com.flowpowered.nbt.ByteArrayTag;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.serialization.DefaultChunkSerializer;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.NotNull;

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
        return readChunkAt(world, pos);
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return new CEChunk(world, pos);
        Optional<ByteArrayTag> tag = slimeChunk.getExtraData().getAsByteArrayTag("craftengine");
        if (tag.isEmpty()) return new CEChunk(world, pos);
        try {
            CompoundTag compoundTag = NBT.fromBytes(tag.get().getValue());
            if (compoundTag == null) return new CEChunk(world, pos);
            return DefaultChunkSerializer.deserialize(world, pos, compoundTag);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read chunk tag from slime world. " + pos, e);
        }
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        CompoundTag nbt = DefaultChunkSerializer.serialize(chunk);
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
