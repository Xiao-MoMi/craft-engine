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

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.InactiveCustomBlock;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.ReadableContainer;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.LongArrayTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

public class SectionSerializer {

    @Nullable
    public static CompoundTag serialize(@NotNull CESection section) {
        ReadableContainer.Serialized<ImmutableBlockState> serialized = section.statesContainer().serialize(null, PalettedContainer.PaletteProvider.CUSTOM_BLOCK_STATE);
        ListTag palettes = new ListTag();
        List<ImmutableBlockState> states = serialized.paletteEntries();
        if (states.size() == 1 && states.get(0) == EmptyBlock.INSTANCE.getDefaultState()) {
            return null;
        }
        CompoundTag sectionNbt = new CompoundTag();
        sectionNbt.putByte("y", (byte) section.sectionY());
        CompoundTag blockStates = new CompoundTag();
        sectionNbt.put("block_states", blockStates);
        for (ImmutableBlockState state : states) {
            palettes.add(state.getNbtToSave());
        }
        blockStates.put("palette", palettes);
        serialized.storage().ifPresent(data -> blockStates.put("data", new LongArrayTag(data.toArray())));
        return sectionNbt;
    }

    @Nullable
    public static CESection deserialize(@NotNull CompoundTag sectionNbt) {
        CompoundTag blockStates = sectionNbt.getCompound("block_states");
        if (blockStates == null) {
            return null;
        }
        ListTag palettes = blockStates.getList("palette");
        List<ImmutableBlockState> paletteEntries = new ArrayList<>(palettes.size());
        for (Tag tag : palettes) {
            CompoundTag palette = (CompoundTag) tag;
            String id = palette.getString("id");
            CompoundTag data = palette.getCompound("properties");
            Key key = Key.of(id);
            Holder<CustomBlock> owner = BuiltInRegistries.BLOCK.get(key).orElseGet(() -> {
                Holder.Reference<CustomBlock> holder = ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(
                        new ResourceKey<>(BuiltInRegistries.BLOCK.key().location(), key));
                InactiveCustomBlock inactiveBlock = new InactiveCustomBlock(key, holder);
                holder.bindValue(inactiveBlock);
                return holder;
            });
            ImmutableBlockState state = owner.value().getBlockState(data);
            paletteEntries.add(state);
        }
        long[] data = blockStates.getLongArray("data");
        ReadableContainer.Serialized<ImmutableBlockState> serialized = new ReadableContainer.Serialized<>(paletteEntries,
                data == null ? Optional.empty() : Optional.of(LongStream.of(data)));
        PalettedContainer<ImmutableBlockState> palettedContainer = PalettedContainer.read(null, PalettedContainer.PaletteProvider.CUSTOM_BLOCK_STATE, serialized);
        return new CESection(sectionNbt.getByte("y"), palettedContainer);
    }
}
