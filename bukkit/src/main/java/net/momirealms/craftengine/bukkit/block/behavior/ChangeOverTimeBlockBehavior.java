package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.List;

public final class ChangeOverTimeBlockBehavior extends BukkitBlockBehavior implements RandomTickBlock {
    public static final BlockBehaviorFactory<ChangeOverTimeBlockBehavior> FACTORY = new Factory();
    public final float changeSpeed;
    public final String nextBlock;
    public final LazyReference<BlockStateWrapper> lazyState;
    public final List<String> excludedProperties;

    private ChangeOverTimeBlockBehavior(BlockDefinition blockDefinition,
                                        float changeSpeed,
                                        String nextBlock,
                                        List<String> excludedProperties) {
        super(blockDefinition);
        this.changeSpeed = changeSpeed;
        this.nextBlock = nextBlock;
        this.excludedProperties = excludedProperties;
        this.lazyState = LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(this.nextBlock));
    }

    public BlockStateWrapper nextState() {
        return this.lazyState.get();
    }

    public CompoundTag filter(CompoundTag properties) {
        for (String property : this.excludedProperties) {
            properties.remove(property);
        }
        return properties;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        if (RandomUtils.generateRandomFloat(0F, 1F) >= this.changeSpeed) return;
        Object blockState = args[0];
        BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(state -> {
            BlockStateWrapper nextState = this.nextState();
            if (nextState == null) return;
            nextState = nextState.withProperties(filter(state.propertiesNbt()));
            CraftEventFactoryProxy.INSTANCE.handleBlockFormEvent(args[1], args[2], nextState.minecraftState(), UpdateFlags.UPDATE_ALL);
        });
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    private static class Factory implements BlockBehaviorFactory<ChangeOverTimeBlockBehavior> {
        private static final String[] CHANGE_SPEED = new String[] {"change_speed", "change-speed"};
        private static final String[] NEXT_BLOCK = new String[] {"next_block", "next-block"};
        private static final String[] EXCLUDED_PROPERTIES = new String[] {"excluded_properties", "excluded-properties"};

        @Override
        public ChangeOverTimeBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new ChangeOverTimeBlockBehavior(
                    block,
                    section.getFloat(CHANGE_SPEED, 0.057F),
                    section.getNonEmptyString(NEXT_BLOCK),
                    section.getStringList(EXCLUDED_PROPERTIES)
            );
        }
    }
}
