package net.momirealms.craftengine.core.block;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InactiveCustomBlock extends AbstractCustomBlock {
    private final Map<CompoundTag, ImmutableBlockState> cachedData = new HashMap<>();

    public InactiveCustomBlock(Key id, Holder.Reference<CustomBlock> holder) {
        super(id, holder, Map.of(), Map.of(), Map.of(), BlockSettings.of(), Map.of(), List.of(), null);
    }

    @Override
    protected void applyPlatformSettings() {
    }

    @Override
    public ImmutableBlockState getBlockState(CompoundTag nbt) {
        return this.cachedData.computeIfAbsent(nbt, k -> {
            ImmutableBlockState state = new ImmutableBlockState(super.holder, new Reference2ObjectArrayMap<>());
            state.setNbtToSave(state.toNbtToSave(nbt));
            return state;
        });
    }
}