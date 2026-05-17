package net.momirealms.craftengine.bukkit.world.chunk;

import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftChunkProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ChunkPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkAccessProxy;
import org.bukkit.persistence.PersistentDataContainer;

public final class BukkitChunkAccess implements Chunk {
    private final Object chunkAccess;

    public BukkitChunkAccess(Object chunkAccess) {
        this.chunkAccess = chunkAccess;
    }

    @Override
    public Object minecraftChunk() {
        return this.chunkAccess;
    }

    @Override
    public org.bukkit.Chunk platformChunk() {
        return CraftChunkProxy.INSTANCE.newInstance(this.chunkAccess);
    }

    @Override
    public ChunkPos pos() {
        Object chunkPos = ChunkAccessProxy.INSTANCE.getChunkPos(this.chunkAccess);
        return new ChunkPos(ChunkPosProxy.INSTANCE.getX(chunkPos), ChunkPosProxy.INSTANCE.getZ(chunkPos));
    }

    public PersistentDataContainer getPersistentDataContainer() {
        return ChunkAccessProxy.INSTANCE.getPersistentDataContainer(this.chunkAccess);
    }
}
