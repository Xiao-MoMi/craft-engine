package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import static net.momirealms.craftengine.core.block.UpdateFlags.*;

public final class MultiHighBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<MultiHighBlockBehavior> FACTORY = new Factory();
    public final IntegerProperty property;

    private MultiHighBlockBehavior(BlockDefinition blockDefinition, IntegerProperty property) {
        super(blockDefinition);
        this.property = property;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (customState == null || customState.isEmpty()) {
            return BlocksProxy.AIR$defaultState;
        }
        MultiHighBlockBehavior behavior = customState.behavior().getFirst(MultiHighBlockBehavior.class);
        if (behavior == null) {
            return BlocksProxy.AIR$defaultState;
        }
        IntegerProperty property = behavior.property;
        int value = customState.get(property);
        Object direction = args[updateShape$direction];
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        if (direction == DirectionProxy.UP && value != property.max) {
            Object abovePos = LocationUtils.above(blockPos);
            Object aboveState = BlockGetterProxy.INSTANCE.getBlockState(level, abovePos);
            ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(aboveState).orElse(null);
            if (state == null) {
                playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
            MultiHighBlockBehavior aboveBehavior = state.behavior().getFirst(MultiHighBlockBehavior.class);
            if (aboveBehavior == null || aboveBehavior.property != property) {
                playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
            Integer aboveValue = state.get(property);
            if (value + 1 != aboveValue) {
                playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
        } else if (direction == DirectionProxy.DOWN && value != property.min) {
            Object belowPos = LocationUtils.below(blockPos);
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
            ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(belowState).orElse(null);
            if (state == null) {
                playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
            MultiHighBlockBehavior belowBehavior = state.behavior().getFirst(MultiHighBlockBehavior.class);
            if (belowBehavior == null || belowBehavior.property != property) {
                playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
            Integer belowValue = state.get(property);
            if (value - 1 != belowValue) {
                playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
        }
        return blockState;
    }

    public static void playBreakEffect(ImmutableBlockState customState, Object blockPos, Object level) {
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
        WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
        world.playBlockSound(position, customState.settings().sounds().breakSound());
        LevelAccessorProxy.INSTANCE.levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId());
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args) {
        Object player = args[3];
        Object state = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null || blockState.isEmpty()) {
            return state;
        }
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(ServerPlayerProxy.INSTANCE.getBukkitEntity(player));
        if (serverPlayer == null) {
            return state;
        }
        Item item = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (serverPlayer.canInstabuild() || !BlockStateUtils.isCorrectTool(blockState, item)) {
            preventDropFromBasePart(args[0], args[1], blockState, player);
        }
        return state;
    }

    private void preventDropFromBasePart(Object level, Object pos, ImmutableBlockState state, Object player) {
        MultiHighBlockBehavior behavior = state.behavior().getFirst(MultiHighBlockBehavior.class);
        if (behavior == null) {
            return;
        }
        IntegerProperty property = behavior.property;
        int value = state.get(property);
        if (value == property.min) {
            return;
        }
        Object basePos = LocationUtils.below(pos, value - property.min);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, basePos);
        ImmutableBlockState baseState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (baseState == null || baseState.isEmpty()) {
            return;
        }
        MultiHighBlockBehavior baseBehavior = baseState.behavior().getFirst(MultiHighBlockBehavior.class);
        if (baseBehavior == null) {
            return;
        }
        IntegerProperty baseProperty = baseBehavior.property;
        if (baseState.get(baseProperty) != baseProperty.min) {
            return;
        }
        Object emptyState = FluidStateProxy.INSTANCE.getType(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == FluidsProxy.WATER
                ? BlocksProxy.WATER$defaultState
                : BlocksProxy.AIR$defaultState;
        LevelWriterProxy.INSTANCE.setBlock(level, basePos, emptyState, UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_SUPPRESS_DROPS);
        LevelUtils.levelEvent(level, player, WorldEvents.BLOCK_BREAK_EFFECT, basePos, baseState.customBlockState().registryId());
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object world = args[1];
        Object blockPos = args[2];
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState == null || customState.isEmpty()) {
            return false;
        }
        MultiHighBlockBehavior behavior = customState.behavior().getFirst(MultiHighBlockBehavior.class);
        if (behavior == null) {
            return false;
        }
        IntegerProperty property = behavior.property;
        int value = customState.get(property);
        if (value != property.min && value != property.max) {
            Object aboveState = BlockGetterProxy.INSTANCE.getBlockState(world, LocationUtils.above(blockPos));
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, LocationUtils.below(blockPos));
            BlockDefinition aboveBlockDefinition = BlockStateUtils.getOptionalCustomBlockState(aboveState).map(blockState -> blockState.owner().value()).orElse(null);
            BlockDefinition belowBlockDefinition = BlockStateUtils.getOptionalCustomBlockState(belowState).map(blockState -> blockState.owner().value()).orElse(null);
            return aboveBlockDefinition == behavior.blockDefinition && belowBlockDefinition == behavior.blockDefinition;
        } else if (value == property.max) {
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, LocationUtils.below(blockPos));
            BlockDefinition belowBlockDefinition = BlockStateUtils.getOptionalCustomBlockState(belowState).map(blockState -> blockState.owner().value()).orElse(null);
            return belowBlockDefinition == behavior.blockDefinition;
        }
        return true;
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args) {
        Object blockState = args[2];
        Object pos = args[1];
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (state == null) {
            return;
        }
        MultiHighBlockBehavior behavior = state.behavior().getFirst(MultiHighBlockBehavior.class);
        if (behavior == null) {
            return;
        }
        IntegerProperty property = behavior.property;
        for (int i = property.min + 1; i <= property.max; i++) {
            LevelWriterProxy.INSTANCE.setBlock(args[0], LocationUtils.above(pos, i), state.with(property, i).customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
        }
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        return this.property.max - this.property.min > 0;
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        MultiHighBlockBehavior behavior = state.behavior().getFirst(MultiHighBlockBehavior.class);
        if (behavior == null) {
            return false;
        }
        IntegerProperty property = behavior.property;
        if (pos.y() >= accessor.worldHeight().getMaxBuildHeight() - property.max) {
            return false;
        }
        for (int i = property.min + 1; i <= property.max; i++) {
            if (!accessor.getBlockState(pos.relative(Direction.UP, i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World world  = context.getLevel();
        BlockPos pos = context.getClickedPos();
        MultiHighBlockBehavior behavior = state.behavior().getFirst(MultiHighBlockBehavior.class);
        if (behavior == null) {
            return null;
        }
        IntegerProperty property = behavior.property;
        if (pos.y() >= context.getLevel().worldHeight().getMaxBuildHeight() - property.max) {
            return null;
        }
        for (int i = property.min + 1; i <= property.max; i++) {
            if (!world.getBlock(pos.relative(Direction.UP, i)).canBeReplaced(context)) {
                return null;
            }
        }
        return state.with(property, property.min);
    }

    private static class Factory implements BlockBehaviorFactory<MultiHighBlockBehavior> {

        @Override
        public MultiHighBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new MultiHighBlockBehavior(
                    block,
                    (IntegerProperty) BlockBehaviorFactory.getProperty(section.path(), block, section.getNonNullString("property"), Integer.class)
            );
        }
    }
}
