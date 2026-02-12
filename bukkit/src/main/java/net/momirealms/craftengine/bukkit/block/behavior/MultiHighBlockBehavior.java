package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class MultiHighBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<MultiHighBlockBehavior> FACTORY = new Factory();
    public final IntegerProperty property;

    public MultiHighBlockBehavior(CustomBlock customBlock, IntegerProperty property) {
        super(customBlock);
        this.property = property;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (customState == null || customState.isEmpty()) {
            return MBlocks.AIR$defaultState;
        }
        MultiHighBlockBehavior behavior = customState.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return MBlocks.AIR$defaultState;
        }
        IntegerProperty property = behavior.property;
        int value = customState.get(property);
        Object direction = args[updateShape$direction];
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        if (direction == CoreReflections.instance$Direction$UP && value != property.max) {
            Object abovePos = LocationUtils.above(blockPos);
            Object aboveState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, abovePos);
            ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(aboveState).orElse(null);
            if (state == null) {
                playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
            MultiHighBlockBehavior aboveBehavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
            if (aboveBehavior == null || aboveBehavior.property != property) {
                playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
            Integer aboveValue = state.get(property);
            if (value + 1 != aboveValue) {
                playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
        } else if (direction == CoreReflections.instance$Direction$DOWN && value != property.min) {
            Object belowPos = LocationUtils.below(blockPos);
            Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, belowPos);
            ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(belowState).orElse(null);
            if (state == null) {
                playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
            MultiHighBlockBehavior belowBehavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
            if (belowBehavior == null || belowBehavior.property != property) {
                playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
            Integer belowValue = state.get(property);
            if (value - 1 != belowValue) {
                playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
        }
        return blockState;
    }

    public static void playBreakEffect(ImmutableBlockState customState, Object blockPos, Object level) {
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
        WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
        world.playBlockSound(position, customState.settings().sounds().breakSound());
        FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId());
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object player = args[3];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[2]).orElse(null);
        if (blockState == null || blockState.isEmpty()) {
            return superMethod.call();
        }
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(player));
        if (serverPlayer == null) {
            return superMethod.call();
        }
        Item<ItemStack> item = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (serverPlayer.canInstabuild() || !BlockStateUtils.isCorrectTool(blockState, item)) {
            preventDropFromBasePart(args[0], args[1], blockState, player);
        }
        return superMethod.call();
    }

    private void preventDropFromBasePart(Object level, Object pos, ImmutableBlockState state, Object player) {
        MultiHighBlockBehavior behavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return;
        }
        IntegerProperty property = behavior.property;
        int value = state.get(property);
        if (value == property.min) {
            return;
        }
        Object basePos = LocationUtils.below(pos, value - property.min);
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, basePos);
        ImmutableBlockState baseState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (baseState == null || baseState.isEmpty()) {
            return;
        }
        Optional<MultiHighBlockBehavior> baseBehavior = baseState.behavior().getAs(MultiHighBlockBehavior.class);
        if (baseBehavior.isEmpty()) {
            return;
        }
        IntegerProperty baseProperty = baseBehavior.get().property;
        if (baseState.get(baseProperty) != baseProperty.min) {
            return;
        }
        Object emptyState = FastNMS.INSTANCE.method$FluidState$getType(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(blockState)) == MFluids.WATER
                ? MBlocks.WATER$defaultState
                : MBlocks.AIR$defaultState;
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, basePos, emptyState, UpdateOption.builder().updateSuppressDrops().updateClients().updateNeighbors().build().flags());
        FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, player, WorldEvents.BLOCK_BREAK_EFFECT, basePos, baseState.customBlockState().registryId());
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object world = args[1];
        Object blockPos = args[2];
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState == null || customState.isEmpty()) {
            return false;
        }
        MultiHighBlockBehavior behavior = customState.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return false;
        }
        IntegerProperty property = behavior.property;
        int value = customState.get(property);
        if (value != property.min && value != property.max) {
            Object aboveState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, LocationUtils.above(blockPos));
            Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, LocationUtils.below(blockPos));
            CustomBlock aboveCustomBlock = BlockStateUtils.getOptionalCustomBlockState(aboveState).map(blockState -> blockState.owner().value()).orElse(null);
            CustomBlock belowCustomBlock = BlockStateUtils.getOptionalCustomBlockState(belowState).map(blockState -> blockState.owner().value()).orElse(null);
            return aboveCustomBlock == behavior.customBlock && belowCustomBlock == behavior.customBlock;
        } else if (value == property.max) {
            Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, LocationUtils.below(blockPos));
            CustomBlock belowCustomBlock = BlockStateUtils.getOptionalCustomBlockState(belowState).map(blockState -> blockState.owner().value()).orElse(null);
            return belowCustomBlock == behavior.customBlock;
        }
        return true;
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[2];
        Object pos = args[1];
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (state == null) {
            return;
        }
        MultiHighBlockBehavior behavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return;
        }
        IntegerProperty property = behavior.property;
        for (int i = property.min + 1; i <= property.max; i++) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(args[0], LocationUtils.above(pos, i), state.with(property, i).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        }
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        return this.property.max - this.property.min > 0;
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        MultiHighBlockBehavior behavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
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
        MultiHighBlockBehavior behavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
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
        public MultiHighBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String propertyName = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("property"), "warning.config.block.behavior.multi_high.missing_property_name");
            IntegerProperty property = (IntegerProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty(propertyName), () -> {
                throw new LocalizedResourceConfigException("warning.config.block.behavior.multi_high.missing_property", propertyName);
            });
            return new MultiHighBlockBehavior(block, property);
        }
    }
}
