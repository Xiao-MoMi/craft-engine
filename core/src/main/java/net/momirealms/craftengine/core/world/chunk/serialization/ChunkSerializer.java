package net.momirealms.craftengine.core.world.chunk.serialization;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkSerializer {

    @Nullable
    public static CompoundTag serialize(@NotNull CEChunk chunk) {
        ListTag sections = new ListTag();
        CESection[] ceSections = chunk.sections();
        for (CESection ceSection : ceSections) {
            if (ceSection != null) {
                CompoundTag sectionNbt = SectionSerializer.serialize(ceSection);
                if (sectionNbt != null) {
                    sections.add(sectionNbt);
                }
            }
        }
        if (sections.isEmpty()) return null;
        CompoundTag chunkNbt = new CompoundTag();
        chunkNbt.put("sections", sections);
        chunkNbt.put("entities", new ListTag());
        return chunkNbt;
    }

    @NotNull
    public static CEChunk deserialize(@NotNull CEWorld world, @NotNull ChunkPos pos, @NotNull CompoundTag chunkNbt) {
        ListTag sections = chunkNbt.getList("sections");
        CESection[] sectionArray = new CESection[world.worldHeight().getSectionsCount()];
        for (int i = 0, size = sections.size(); i < size; ++i) {
            CompoundTag sectionTag = sections.getCompound(i);
            CESection ceSection = SectionSerializer.deserialize(sectionTag);
            if (ceSection != null) {
                int sectionIndex = world.worldHeight().getSectionIndexFromSectionY(ceSection.sectionY());
                if (sectionIndex >= 0 && sectionIndex < sectionArray.length) {
                    sectionArray[sectionIndex] = ceSection;
                }
            }
        }
        ListTag entities = chunkNbt.getList("entities");
        return new CEChunk(world, pos, sectionArray, List.of());
    }
}
