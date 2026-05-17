package net.momirealms.craftengine.core.world.chunk.serialization;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DefaultBlockEntitySerializer {
    private DefaultBlockEntitySerializer() {}

    public static ListTag serialize(Collection<BlockEntity> entities) {
        ListTag result = new ListTag();
        for (BlockEntity entity : entities) {
            if (entity.isValid()) {
                result.add(entity.saveAsTag());
            }
        }
        return result;
    }

    public static List<BlockEntity> deserialize(CEChunk chunk, ListTag tag) {
        List<BlockEntity> blockEntities = new ArrayList<>(tag.size());
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag data = tag.getCompound(i);
            BlockPos pos = BlockEntity.readPosAndVerify(data, chunk.chunkPos());
            ImmutableBlockState blockState = chunk.getBlockState(pos);
            if (blockState.hasBlockEntity()) {
                BlockEntity blockEntity = new BlockEntity(pos, blockState);
                blockEntity.loadCustomData(data);
                blockEntities.add(blockEntity);
            } else {
                blockEntities.add(BlockEntity.inactive(pos, blockState, data));
            }
        }
        return blockEntities;
    }
}
