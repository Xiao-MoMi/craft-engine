package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.List;

public final class StrippableBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<StrippableBlockBehavior> FACTORY = new Factory();
    public final String stripped;
    public final LazyReference<BlockStateWrapper> lazyState;
    public final List<String> excludedProperties;

    private StrippableBlockBehavior(BlockDefinition block,
                                    String stripped,
                                    List<String> excludedProperties) {
        super(block);
        this.stripped = stripped;
        this.lazyState = LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(this.stripped));
        this.excludedProperties = excludedProperties;
    }

    public BlockStateWrapper strippedState() {
        return this.lazyState.get();
    }

    public CompoundTag filter(CompoundTag properties) {
        for (String property : this.excludedProperties) {
            properties.remove(property);
        }
        return properties;
    }

    private static class Factory implements BlockBehaviorFactory<StrippableBlockBehavior> {
        private static final String[] EXCLUDED_PROPERTIES = new String[] {"excluded_properties", "excluded-properties"};

        @Override
        public StrippableBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new StrippableBlockBehavior(
                    block,
                    section.getNonNullString("stripped"),
                    section.getStringList(EXCLUDED_PROPERTIES)
            );
        }
    }
}
