package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.AnchorType;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class FaceAttachedHorizontalDirectionalBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<FaceAttachedHorizontalDirectionalBlockBehavior> FACTORY = new Factory();
    public final Property<AnchorType> anchorTypeProperty;
    public final Property<Direction> facingProperty;
    public final List<Object> tagsCanSurviveOn;
    public final LazyReference<Set<Object>> blockStatesCanSurviveOn;
    public final boolean blacklistMode;

    private FaceAttachedHorizontalDirectionalBlockBehavior(BlockDefinition blockDefinition,
                                                           boolean blacklist,
                                                           List<Object> tagsCanSurviveOn,
                                                           LazyReference<Set<Object>> blockStatesCanSurviveOn,
                                                           Property<AnchorType> anchorType,
                                                           Property<Direction> facing) {
        super(blockDefinition);
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
        this.blacklistMode = blacklist;
        this.anchorTypeProperty = anchorType;
        this.facingProperty = facing;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        Direction direction = getConnectedDirection(BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null));
        if (direction == null) return false;
        direction = direction.opposite();
        Object nmsDirection = DirectionUtils.toNMSDirection(direction);
        Object targetPos = BlockPosProxy.INSTANCE.relative(args[2], nmsDirection);
        Object targetState = BlockGetterProxy.INSTANCE.getBlockState(args[1], targetPos);
        return canAttach(args[1], targetPos, nmsDirection, targetState) && mayPlaceOn(targetState);
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.axis() == Direction.Axis.Y) {
                state = state
                        .with(this.anchorTypeProperty, direction == Direction.UP ? AnchorType.CEILING : AnchorType.FLOOR)
                        .with(this.facingProperty, context.getHorizontalDirection());
            } else {
                state = state
                        .with(this.anchorTypeProperty, AnchorType.WALL)
                        .with(this.facingProperty, direction.opposite());
            }
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(state.customBlockState().minecraftState(), context.getLevel().minecraftWorld(), LocationUtils.toBlockPos(context.getClickedPos()))) {
                return state;
            }
        }
        return null;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Direction direction = getConnectedDirection(BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null));
        if (direction == null) return BlocksProxy.AIR$defaultState;
        if (DirectionUtils.toNMSDirection(direction.opposite()) == args[updateShape$direction] && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(args[0], args[updateShape$level], args[updateShape$blockPos])) {
            return BlocksProxy.AIR$defaultState;
        }
        return super.updateShape(thisBlock, args);
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

    public static boolean canAttach(Object level, Object targetPos, Object direction, Object targetState) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(
                targetState, level, targetPos,
                DirectionProxy.INSTANCE.getOpposite(direction),
                SupportTypeProxy.FULL
        );
    }

    @Nullable
    public static Direction getConnectedDirection(ImmutableBlockState state) {
        if (state == null) return null;
        FaceAttachedHorizontalDirectionalBlockBehavior behavior = state.behavior().getFirst(FaceAttachedHorizontalDirectionalBlockBehavior.class);
        if (behavior == null) return null;
        return switch (state.get(behavior.anchorTypeProperty)) {
            case CEILING -> Direction.DOWN;
            case FLOOR -> Direction.UP;
            default -> state.get(behavior.facingProperty);
        };
    }

    private static class Factory implements BlockBehaviorFactory<FaceAttachedHorizontalDirectionalBlockBehavior> {

        @Override
        public FaceAttachedHorizontalDirectionalBlockBehavior create(BlockDefinition block, ConfigSection section) {
            AbstractCanSurviveBlockBehavior.TagsAndState attached = AbstractCanSurviveBlockBehavior.readTagsAndState(section, "attached");
            return new FaceAttachedHorizontalDirectionalBlockBehavior(
                    block,
                    section.getBoolean("blacklist", true),
                    attached.tags(),
                    attached.blockStates(),
                    BlockBehaviorFactory.getProperty(section.path(), block, "face", AnchorType.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class)
            );
        }
    }
}
