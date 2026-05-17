package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

import java.util.List;
import java.util.Set;

public final class DirectionalAttachedBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<DirectionalAttachedBlockBehavior> FACTORY = new Factory();
    public final Property<Direction> facingProperty;
    public final List<Object> tagsCanSurviveOn;
    public final LazyReference<Set<Object>> blockStatesCanSurviveOn;
    public final boolean blacklistMode;

    private DirectionalAttachedBlockBehavior(BlockDefinition blockDefinition,
                                             Property<Direction> facingProperty,
                                             boolean blacklist,
                                             List<Object> tagsCanSurviveOn,
                                             LazyReference<Set<Object>> blockStatesCanSurviveOn) {
        super(blockDefinition);
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.facingProperty = facingProperty;
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
        this.blacklistMode = blacklist;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) return args[0];
        DirectionalAttachedBlockBehavior behavior = state.behavior().getFirst(DirectionalAttachedBlockBehavior.class);
        if (behavior == null) return state;
        Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]).opposite();
        boolean flag = direction == state.get(behavior.facingProperty);
        return flag && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(args[0], args[updateShape$level], args[updateShape$blockPos]) ? BlocksProxy.AIR$defaultState : args[0];
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) return false;
        DirectionalAttachedBlockBehavior behavior = state.behavior().getFirst(DirectionalAttachedBlockBehavior.class);
        if (behavior == null) return false;
        Direction direction = state.get(behavior.facingProperty);
        BlockPos blockPos = LocationUtils.fromBlockPos(args[2]).relative(direction.opposite());
        Object nmsPos = LocationUtils.toBlockPos(blockPos);
        Object nmsState = BlockGetterProxy.INSTANCE.getBlockState(args[1], nmsPos);
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(nmsState, args[1], nmsPos, DirectionUtils.toNMSDirection(direction), SupportTypeProxy.FULL)
                && mayPlaceOn(nmsState);
    }

    private boolean mayPlaceOn(Object state) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(state, tag)) {
                return !this.blacklistMode;
            }
        }
        if (this.blockStatesCanSurviveOn.get().contains(state)) {
            return !this.blacklistMode;
        }
        return this.blacklistMode;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        DirectionalAttachedBlockBehavior behavior = state.behavior().getFirst(DirectionalAttachedBlockBehavior.class);
        if (behavior == null) return null;
        World level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        for (Direction direction : context.getNearestLookingDirections()) {
            state = state.with(behavior.facingProperty, direction.opposite());
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(state.customBlockState().minecraftState(), level.minecraftWorld(), LocationUtils.toBlockPos(clickedPos))) {
                return state;
            }
        }
        return null;
    }

    private static class Factory implements BlockBehaviorFactory<DirectionalAttachedBlockBehavior> {

        @Override
        public DirectionalAttachedBlockBehavior create(BlockDefinition block, ConfigSection section) {
            AbstractCanSurviveBlockBehavior.TagsAndState attached = AbstractCanSurviveBlockBehavior.readTagsAndState(section, "attached");
            return new DirectionalAttachedBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    section.getBoolean("blacklist", true),
                    attached.tags(),
                    attached.blockStates()
            );
        }
    }
}
