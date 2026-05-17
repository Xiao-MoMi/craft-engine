package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class HangingBlockBehavior extends BushBlockBehavior {
    public static final BlockBehaviorFactory<HangingBlockBehavior> FACTORY = new Factory();

    private HangingBlockBehavior(BlockDefinition block,
                                 int delay,
                                 boolean blacklist,
                                 boolean stackable,
                                 int maxHeight,
                                 List<Object> tagsCanSurviveOn,
                                 LazyReference<Set<Object>> blockStatesCanSurviveOn) {
        super(block, delay, blacklist, stackable, maxHeight, tagsCanSurviveOn, blockStatesCanSurviveOn);
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        Object belowPos = LocationUtils.above(blockPos);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
        return mayPlaceOn(belowState, level, belowPos);
    }

    protected boolean mayStackOn(Object world, Object abovePos) {
        int count = 1;
        Object cursorPos = LocationUtils.below(abovePos);

        while (count < this.maxHeight) {
            Object aboveState = BlockGetterProxy.INSTANCE.getBlockState(world, cursorPos);
            Optional<ImmutableBlockState> belowCustomState = BlockStateUtils.getOptionalCustomBlockState(aboveState);
            if (belowCustomState.isPresent() && belowCustomState.get().owner().value() == super.blockDefinition) {
                count++;
                cursorPos = LocationUtils.above(cursorPos);
            } else {
                break;
            }
        }
        return count < this.maxHeight;
    }

    private static class Factory implements BlockBehaviorFactory<HangingBlockBehavior> {
        private static final String[] MAX_HEIGHT = new String[] {"max_height", "max-height"};

        @Override
        public HangingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            TagsAndState tagsAndState = readTagsAndState(section, "above");
            return new HangingBlockBehavior(
                    block,
                    section.getInt("delay", 0),
                    section.getBoolean("blacklist"),
                    section.getBoolean("stackable"),
                    section.getInt(MAX_HEIGHT),
                    tagsAndState.tags(),
                    tagsAndState.blockStates()
            );
        }
    }
}
