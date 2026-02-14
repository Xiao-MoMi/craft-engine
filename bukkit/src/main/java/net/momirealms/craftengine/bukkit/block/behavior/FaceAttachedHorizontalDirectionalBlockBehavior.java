package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.AnchorType;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

public class FaceAttachedHorizontalDirectionalBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<FaceAttachedHorizontalDirectionalBlockBehavior> FACTORY = new Factory();
    private final Property<AnchorType> anchorTypeProperty;
    private final Property<HorizontalDirection> facingProperty;
    private final List<Object> tagsCanSurviveOn;
    private final Set<Object> blockStatesCanSurviveOn;
    private final Set<String> customBlocksCansSurviveOn;
    private final boolean blacklistMode;

    public FaceAttachedHorizontalDirectionalBlockBehavior(CustomBlock customBlock,
                                                          boolean blacklist,
                                                          List<Object> tagsCanSurviveOn,
                                                          Set<Object> blockStatesCanSurviveOn,
                                                          Set<String> customBlocksCansSurviveOn,
                                                          Property<AnchorType> anchorType,
                                                          Property<HorizontalDirection> facing) {
        super(customBlock);
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
        this.customBlocksCansSurviveOn = customBlocksCansSurviveOn;
        this.blacklistMode = blacklist;
        this.anchorTypeProperty = anchorType;
        this.facingProperty = facing;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Direction direction = getConnectedDirection(BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null));
        if (direction == null) return false;
        direction = direction.opposite();
        Object nmsDirection = DirectionUtils.toNMSDirection(direction);
        Object targetPos = BlockPosProxy.INSTANCE.relative(args[2], nmsDirection);
        Object targetState = BlockGetterProxy.INSTANCE.getBlockState(args[1], targetPos);
        return canAttach(args[1], targetPos, nmsDirection, targetState) && mayPlaceOn(targetState);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Property<AnchorType> face = (Property<AnchorType>) state.owner().value().getProperty("face");
        Property<HorizontalDirection> facing = (Property<HorizontalDirection>) state.owner().value().getProperty("facing");
        if (face == null || facing == null) return null;
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.axis() == Direction.Axis.Y) {
                state = state
                        .with(face, direction == Direction.UP ? AnchorType.CEILING : AnchorType.FLOOR)
                        .with(facing, context.getHorizontalDirection().toHorizontalDirection());
            } else {
                state = state.with(face, AnchorType.WALL).with(facing, direction.opposite().toHorizontalDirection());
            }
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(state.customBlockState().literalObject(), context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()))) {
                return state;
            }
        }
        return null;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Direction direction = getConnectedDirection(BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null));
        if (direction == null) return BlocksProxy.AIR$defaultState;
        if (DirectionUtils.toNMSDirection(direction.opposite()) == args[updateShape$direction] && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(args[0], args[updateShape$level], args[updateShape$blockPos])) {
            return BlocksProxy.AIR$defaultState;
        }
        return superMethod.call();
    }

    private boolean mayPlaceOn(Object state) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(state, tag)) {
                return !this.blacklistMode;
            }
        }
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) {
            if (!this.blockStatesCanSurviveOn.isEmpty() && this.blockStatesCanSurviveOn.contains(state)) {
                return !this.blacklistMode;
            }
        } else {
            ImmutableBlockState belowCustomState = optionalCustomState.get();
            if (this.customBlocksCansSurviveOn.contains(belowCustomState.owner().value().id().toString())) {
                return !this.blacklistMode;
            }
            if (this.customBlocksCansSurviveOn.contains(belowCustomState.toString())) {
                return !this.blacklistMode;
            }
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
        FaceAttachedHorizontalDirectionalBlockBehavior behavior = state.behavior().getAs(FaceAttachedHorizontalDirectionalBlockBehavior.class).orElse(null);
        if (behavior == null) return null;
        return switch (state.get(behavior.anchorTypeProperty)) {
            case CEILING -> Direction.DOWN;
            case FLOOR -> Direction.UP;
            default -> state.get(behavior.facingProperty).toDirection();
        };
    }

    private static class Factory implements BlockBehaviorFactory<FaceAttachedHorizontalDirectionalBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public FaceAttachedHorizontalDirectionalBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<AnchorType> anchorType = (Property<AnchorType>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("face"), "warning.config.block.behavior.face_attached_horizontal_directional.missing_face");
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.face_attached_horizontal_directional.missing_facing");
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = DirectionalAttachedBlockBehavior.readTagsAndState(arguments);
            boolean blacklistMode = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blacklist", true), "blacklist");
            return new FaceAttachedHorizontalDirectionalBlockBehavior(block, blacklistMode, tuple.left(), tuple.mid(), tuple.right(), anchorType, facing);
        }
    }
}
