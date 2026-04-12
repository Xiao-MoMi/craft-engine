package net.momirealms.craftengine.core.world.chunk.serialization;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityType;
import net.momirealms.craftengine.core.block.entity.InactiveBlockEntity;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
            String idString = data.getString("id");
            if (idString == null) continue;
            Key id = Key.of(idString);
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id);
            BlockPos pos = BlockEntity.readPosAndVerify(data, chunk.chunkPos());
            ImmutableBlockState blockState = chunk.getBlockState(pos);
            if (type == null) {
                Debugger.BLOCK.debug(() -> "Unknown block entity type: " + id);
                BlockEntity blockEntity = new InactiveBlockEntity(pos, blockState, data);
                blockEntities.add(blockEntity);
            } else {
                if (blockState.blockEntityType() == type) {
                    Optional<EntityBlockBehavior> entityBlockBehavior = blockState.behavior().getAs(EntityBlockBehavior.class);
                    if (entityBlockBehavior.isPresent()) {
                        BlockEntity blockEntity = entityBlockBehavior.get().createBlockEntity(pos, blockState);
                        if (blockEntity != null) {
                            blockEntity.loadCustomData(data);
                            blockEntities.add(blockEntity);
                        }
                    }
                }
            }
        }
        return blockEntities;
    }
}
