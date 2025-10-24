package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.*;
import java.util.concurrent.Callable;

public class DirectionalAttachedBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Property<?> facingProperty;
    private final boolean isSixDirection;
    private final List<Object> tagsCanSurviveOn;
    private final Set<Object> blockStatesCanSurviveOn;
    private final Set<String> customBlocksCansSurviveOn;
    private final boolean blacklistMode;

    public DirectionalAttachedBlockBehavior(CustomBlock customBlock,
                                            Property<?> facingProperty,
                                            boolean isSixDirection,
                                            boolean blacklist,
                                            List<Object> tagsCanSurviveOn,
                                            Set<Object> blockStatesCanSurviveOn,
                                            Set<String> customBlocksCansSurviveOn) {
        super(customBlock);
        this.facingProperty = facingProperty;
        this.isSixDirection = isSixDirection;
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
        this.customBlocksCansSurviveOn = customBlocksCansSurviveOn;
        this.blacklistMode = blacklist;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) return args[0];
        DirectionalAttachedBlockBehavior behavior = state.behavior().getAs(DirectionalAttachedBlockBehavior.class).orElse(null);
        if (behavior == null) return state;
        boolean flag;
        if (isSixDirection) {
            Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]).opposite();
            flag = direction == state.get(behavior.facingProperty);
        } else {
            HorizontalDirection direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]).opposite().toHorizontalDirection();
            flag = direction == state.get(behavior.facingProperty);
        }
        return flag && !FastNMS.INSTANCE.method$BlockStateBase$canSurvive(args[0], args[updateShape$level], args[updateShape$blockPos]) ? MBlocks.AIR$defaultState : args[0];
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) return false;
        DirectionalAttachedBlockBehavior behavior = state.behavior().getAs(DirectionalAttachedBlockBehavior.class).orElse(null);
        if (behavior == null) return false;
        Direction direction;
        if (isSixDirection) {
            direction = ((Direction) state.get(behavior.facingProperty)).opposite();
        } else {
            direction = ((HorizontalDirection) state.get(behavior.facingProperty)).opposite().toDirection();
        }
        BlockPos blockPos = LocationUtils.fromBlockPos(args[2]).relative(direction);
        Object nmsPos = LocationUtils.toBlockPos(blockPos);
        Object nmsState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(args[1], nmsPos);
        return FastNMS.INSTANCE.method$BlockStateBase$isFaceSturdy(nmsState, args[1], nmsPos, DirectionUtils.toNMSDirection(direction), CoreReflections.Instance.supportType$FULL)
                && mayPlaceOn(nmsState);
    }

    private boolean mayPlaceOn(Object state) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (FastNMS.INSTANCE.method$BlockStateBase$is(state, tag)) {
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

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        DirectionalAttachedBlockBehavior behavior = state.behavior().getAs(DirectionalAttachedBlockBehavior.class).orElse(null);
        if (behavior == null) return null;
        World level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        for (Direction direction : context.getNearestLookingDirections()) {
            if (isSixDirection) {
                state = state.with((Property<Direction>) behavior.facingProperty, direction.opposite());
                if (FastNMS.INSTANCE.method$BlockStateBase$canSurvive(state.customBlockState().literalObject(), level.serverWorld(), LocationUtils.toBlockPos(clickedPos))) {
                    return state;
                }
            } else if (direction.axis().isHorizontal()) {
                state = state.with((Property<HorizontalDirection>) behavior.facingProperty, direction.opposite().toHorizontalDirection());
                if (FastNMS.INSTANCE.method$BlockStateBase$canSurvive(state.customBlockState().literalObject(), level.serverWorld(), LocationUtils.toBlockPos(clickedPos))) {
                    return state;
                }
            }
        }
        return null;
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<?> facing = ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.directional_attached.missing_facing");
            boolean isHorizontalDirection = facing.valueClass() == HorizontalDirection.class;
            boolean isDirection = facing.valueClass() == Direction.class;
            if (!(isHorizontalDirection || isDirection)) {
                throw new LocalizedResourceConfigException("warning.config.block.behavior.directional_attached.missing_facing");
            }
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments);
            boolean blacklistMode = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blacklist", true), "blacklist");
            return new DirectionalAttachedBlockBehavior(block, facing, isDirection, blacklistMode, tuple.left(), tuple.mid(), tuple.right());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static Tuple<List<Object>, Set<Object>, Set<String>> readTagsAndState(Map<String, Object> arguments) {
        List<Object> mcTags = new ArrayList<>();
        for (String tag : MiscUtils.getAsStringList(arguments.getOrDefault("attached-block-tags", List.of()))) {
            mcTags.add(BlockTags.getOrCreate(Key.of(tag)));
        }
        Set<Object> mcBlocks = new HashSet<>();
        Set<String> customBlocks = new HashSet<>();
        for (String blockStateStr : MiscUtils.getAsStringList(arguments.getOrDefault("attached-blocks", List.of()))) {
            int index = blockStateStr.indexOf('[');
            Key blockType = index != -1 ? Key.from(blockStateStr.substring(0, index)) : Key.from(blockStateStr);
            Material material = Registry.MATERIAL.get(new NamespacedKey(blockType.namespace(), blockType.value()));
            if (material != null) {
                if (index == -1) {
                    // vanilla
                    mcBlocks.addAll(BlockStateUtils.getPossibleBlockStates(blockType));
                } else {
                    mcBlocks.add(BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(blockStateStr)));
                }
            } else {
                // custom maybe
                customBlocks.add(blockStateStr);
            }
        }
        return new Tuple<>(mcTags, mcBlocks, customBlocks);
    }
}
