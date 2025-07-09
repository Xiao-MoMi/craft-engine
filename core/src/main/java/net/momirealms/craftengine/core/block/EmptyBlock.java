package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public final class EmptyBlock extends AbstractCustomBlock {
    public static EmptyBlock INSTANCE;
    public static ImmutableBlockState STATE;

    public EmptyBlock(Key id, Holder.Reference<CustomBlock> holder) {
        super(id, holder, Map.of(), Map.of(), Map.of(), BlockSettings.of(), Map.of(), List.of(), null);
        INSTANCE = this;
        STATE = defaultState();
    }

    @Override
    protected void applyPlatformSettings() {
    }
}
