package net.momirealms.craftengine.core.world.chunk.serialization;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.storage.ChunkFactory;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultChunkSerializer {
    private DefaultChunkSerializer() {}

    @Nullable
    public static CompoundTag serialize(@NotNull CEChunk chunk) {
        ListTag sections = new ListTag();
        CESection[] ceSections = chunk.sections();
        for (CESection ceSection : ceSections) {
            if (ceSection != null) {
                CompoundTag sectionNbt = DefaultSectionSerializer.serialize(ceSection);
                if (sectionNbt != null) {
                    sections.add(sectionNbt);
                }
            }
        }
        if (sections.isEmpty()) return null;
        CompoundTag chunkNbt = new CompoundTag();
        chunkNbt.put("sections", sections);
        ListTag blockEntities = DefaultBlockEntitySerializer.serialize(chunk.blockEntities());
        if (!blockEntities.isEmpty()) {
            chunkNbt.put("block_entities", blockEntities);
        }
        ListTag blockEntityRenders = DefaultBlockEntityRendererSerializer.serialize(chunk.constantBlockEntityRendererPositions());
        if (!blockEntityRenders.isEmpty()) {
            chunkNbt.put("block_entity_renderers", blockEntityRenders);
        }
        return chunkNbt;
    }

    @NotNull
    public static CEChunk deserialize(ChunkFactory factory, @NotNull CEWorld world, @NotNull ChunkPos pos, @NotNull CompoundTag chunkNbt) {
        ListTag sections = chunkNbt.getList("sections");
        CESection[] sectionArray = new CESection[world.worldHeight().getSectionsCount()];
        for (int i = 0, size = sections.size(); i < size; ++i) {
            CompoundTag sectionTag = sections.getCompound(i);
            CESection ceSection = DefaultSectionSerializer.deserialize(sectionTag);
            if (ceSection != null) {
                int sectionIndex = world.worldHeight().getSectionIndexFromSectionY(ceSection.sectionY());
                if (sectionIndex >= 0 && sectionIndex < sectionArray.length) {
                    sectionArray[sectionIndex] = ceSection;
                }
            }
        }
        ListTag blockEntities = chunkNbt.getList("block_entities");
        ListTag blockEntityRenders = chunkNbt.getList("block_entity_renderers");
        return factory.create(world, pos, sectionArray, blockEntities, blockEntityRenders);
    }
}
