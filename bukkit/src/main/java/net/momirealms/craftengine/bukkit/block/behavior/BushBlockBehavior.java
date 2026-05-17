package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BushBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<BushBlockBehavior> FACTORY = new Factory();
    public final List<Object> tagsCanSurviveOn;
    public final LazyReference<Set<Object>> blockStatesCanSurviveOn;
    public final boolean blacklistMode;
    public final boolean stackable;
    public final int maxHeight;

    protected BushBlockBehavior(BlockDefinition block,
                                int delay,
                                boolean blacklist,
                                boolean stackable,
                                int maxHeight,
                                List<Object> tagsCanSurviveOn,
                                LazyReference<Set<Object>> blockStatesCanSurviveOn) {
        super(block, delay);
        this.blacklistMode = blacklist;
        this.stackable = stackable;
        this.maxHeight = maxHeight;
        this.tagsCanSurviveOn = List.copyOf(tagsCanSurviveOn);
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
    }

    private static class Factory implements BlockBehaviorFactory<BushBlockBehavior> {
        private static final String[] MAX_HEIGHT = new String[] {"max_height", "max-height"};

        @Override
        public BushBlockBehavior create(BlockDefinition block, ConfigSection section) {
            TagsAndState tagsAndState = readTagsAndState(section, "bottom");
            return new BushBlockBehavior(
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

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        Object belowPos = LocationUtils.below(blockPos);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
        return mayPlaceOn(belowState, level, belowPos);
    }

    boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(belowState, tag)) {
                return !this.blacklistMode;
            }
        }
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
        if (optionalCustomState.isEmpty()) {
            if (this.blockStatesCanSurviveOn.get().contains(belowState)) {
                return !this.blacklistMode;
            }
        } else {
            ImmutableBlockState belowCustomState = optionalCustomState.get();
            if (belowCustomState.owner().value() == super.blockDefinition) {
                if (!this.stackable || this.maxHeight == 1) return false;
                if (this.maxHeight > 1) {
                    return mayStackOn(world, belowPos);
                }
                return true;
            }
            if (this.blockStatesCanSurviveOn.get().contains(belowState)) {
                return !this.blacklistMode;
            }
        }
        return this.blacklistMode;
    }

    protected boolean mayStackOn(Object world, Object belowPos) {
        int count = 1;
        Object cursorPos = LocationUtils.below(belowPos);

        while (count < this.maxHeight) {
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, cursorPos);
            Optional<ImmutableBlockState> belowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (belowCustomState.isPresent() && belowCustomState.get().owner().value() == super.blockDefinition) {
                count++;
                cursorPos = LocationUtils.below(cursorPos);
            } else {
                break;
            }
        }
        return count < this.maxHeight;
    }
}
