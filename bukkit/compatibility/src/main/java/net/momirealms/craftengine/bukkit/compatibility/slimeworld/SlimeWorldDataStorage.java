package net.momirealms.craftengine.bukkit.compatibility.slimeworld;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.serialization.DefaultChunkSerializer;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

public final class SlimeWorldDataStorage implements WorldDataStorage {
    private final WeakReference<SlimeWorld> slimeWorld;
    private final SlimeFormatStorageAdaptor adaptor;

    public SlimeWorldDataStorage(SlimeWorld slimeWorld, SlimeFormatStorageAdaptor adaptor) {
        this.slimeWorld = new WeakReference<>(slimeWorld);
        this.adaptor = adaptor;
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
        Object tag = slimeChunk.getExtraData().get("craftengine");
        if (tag == null) return new CEChunk(world, pos);
        try {
            CompoundTag compoundTag = NBT.fromBytes(this.adaptor.byteArrayTagToBytes(tag));
            if (compoundTag == null) return new CEChunk(world, pos);
            return DefaultChunkSerializer.deserialize(world, pos, compoundTag);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chunk tag from slime world " + getWorld().getName() + " " + pos, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        CompoundTag nbt = DefaultChunkSerializer.serialize(chunk);
        if (nbt == null) {
            slimeChunk.getExtraData().remove("craftengine");
        } else {
            try {
                Object tag = this.adaptor.bytesToByteArrayTag(NBT.toBytes(nbt));
                Map<String, Object> data2 = (Map) slimeChunk.getExtraData();
                data2.put("craftengine", tag);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write chunk tag to slime world " + getWorld().getName() + " "  + pos, e);
            }
        }
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        slimeChunk.getExtraData().remove("craftengine");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
