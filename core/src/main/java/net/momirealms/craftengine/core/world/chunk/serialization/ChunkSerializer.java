/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
