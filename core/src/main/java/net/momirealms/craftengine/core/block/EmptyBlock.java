package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class EmptyBlock extends CustomBlock {
    public static EmptyBlock INSTANCE;

    public EmptyBlock(Key id, Holder.Reference<CustomBlock> holder) {
        super(id, holder, Map.of(), Map.of(), Map.of(), BlockSettings.of(), null, null);
        INSTANCE = this;
    }

    @Override
    protected void applyPlatformSettings() {
    }
}
