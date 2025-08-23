package net.momirealms.craftengine.core.world.chunk.serialization;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.InactiveCustomBlock;
import net.momirealms.craftengine.core.plugin.config.Config;
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

public final class DefaultSectionSerializer {

    @Nullable
    public static CompoundTag serialize(@NotNull CESection section) {
        ReadableContainer.Serialized<ImmutableBlockState> serialized = section.statesContainer().serialize(null, PalettedContainer.PaletteProvider.CUSTOM_BLOCK_STATE);
        ListTag palettes = new ListTag();
        List<ImmutableBlockState> states = serialized.paletteEntries();
        if (states.size() == 1 && states.getFirst() == EmptyBlock.STATE) {
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
            Key key;
            if (Config.handleInvalidBlock()) {
                String converted = Config.blockMappings().get(id);
                if (converted == null) {
                    key = Key.of(id);
                } else if (converted.isEmpty()) {
                    paletteEntries.add(EmptyBlock.STATE);
                    continue;
                } else {
                    key = Key.of(converted);
                }
            } else {
                key = Key.of(id);
            }
            Holder<CustomBlock> owner = BuiltInRegistries.BLOCK.get(key).orElseGet(() -> {
                Holder.Reference<CustomBlock> holder = ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(
                        ResourceKey.create(BuiltInRegistries.BLOCK.key().location(), key));
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
