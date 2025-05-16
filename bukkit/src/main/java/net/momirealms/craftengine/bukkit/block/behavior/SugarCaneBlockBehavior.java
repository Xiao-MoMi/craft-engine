package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.shared.block.BlockBehavior;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class SugarCaneBlockBehavior extends BushBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private static final List<Object> WATER = List.of(Reflections.instance$Fluids$WATER, Reflections.instance$Fluids$FLOWING_WATER);
    private static final List<Object> LAVA = List.of(Reflections.instance$Fluids$LAVA, Reflections.instance$Fluids$FLOWING_LAVA);
    private static final List<Object> HORIZON_DIRECTIONS = List.of(Reflections.instance$Direction$NORTH, Reflections.instance$Direction$EAST, Reflections.instance$Direction$SOUTH, Reflections.instance$Direction$WEST);
    private final int maxHeight;
    private final boolean nearWater;
    private final boolean nearLava;
    private final IntegerProperty ageProperty;
    private final float growSpeed;

    public SugarCaneBlockBehavior(CustomBlock customBlock, List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn, Property<Integer> ageProperty,
                                  int maxHeight, boolean nearWater, boolean nearLava, float growSpeed) {
        super(customBlock, tagsCanSurviveOn, blocksCansSurviveOn, customBlocksCansSurviveOn);
        this.nearWater = nearWater;
        this.nearLava = nearLava;
        this.maxHeight = maxHeight;
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        if (!canSurvive(thisBlock, blockState, level, blockPos)) {
            int stateId = BlockStateUtils.blockStateToId(blockState);
            ImmutableBlockState currentState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
            if (currentState != null && !currentState.isEmpty()) {
                // break the sugar cane
                FastNMS.INSTANCE.method$Level$removeBlock(level, blockPos, false);
                net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
                WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(LocationUtils.fromBlockPos(blockPos)));
                ContextHolder.Builder builder = ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position);
                for (Item<Object> item : currentState.getDrops(builder, world, null)) {
                    world.dropItemNaturally(position, item);
                }
                world.playBlockSound(position, currentState.sounds().breakSound());
                FastNMS.INSTANCE.method$Level$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, stateId);
            }
        }
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world;
        Object blockPos;
        if (VersionHelper.isOrAbove1_21_2()) {
            world = args[1];
            blockPos = args[3];
        } else {
            world = args[3];
            blockPos = args[4];
        }
        Reflections.method$LevelAccessor$scheduleTick.invoke(world, blockPos, thisBlock, 1);
        // return state, do not call super.
        return superMethod.call();
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        // above block is empty
        if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.above(blockPos)) == Reflections.instance$Blocks$AIR$defaultState) {
            int currentHeight = 1;
            BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
            ImmutableBlockState currentState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
            if (currentState != null && !currentState.isEmpty()) {
                while (true) {
                    Object belowPos = LocationUtils.toBlockPos(currentPos.x(), currentPos.y() - currentHeight, currentPos.z());
                    Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, belowPos);
                    ImmutableBlockState belowImmutableState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(belowState));
                    if (belowImmutableState != null && !belowImmutableState.isEmpty() && belowImmutableState.owner() == currentState.owner()) {
                        currentHeight++;
                    } else {
                        break;
                    }
                }
            } else {
                return;
            }

            if (currentHeight < this.maxHeight) {
                int age = currentState.get(ageProperty);
                if (age >= this.ageProperty.max || RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    Object abovePos = LocationUtils.above(blockPos);
                    if (VersionHelper.isOrAbove1_21_5()) {
                        Reflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, abovePos, super.customBlock.defaultState().customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
                    } else {
                        Reflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, abovePos, super.customBlock.defaultState().customBlockState().handle());
                    }
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, this.ageProperty.min).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
                } else if (RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, age + 1).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
                }
            }
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y - 1, z);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        int id = BlockStateUtils.blockStateToId(belowState);
        // 如果下方是同种方块
        if (!BlockStateUtils.isVanillaBlock(id)) {
            ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(id);
            if (immutableBlockState.owner().value() == super.customBlock) {
                return true;
            }
        }
        if (!super.mayPlaceOn(belowState, world, belowPos)) {
            return false;
        }
        // 如果不需要依靠流体
        if (!this.nearWater && !this.nearLava) {
            return true;
        }
        // 需要流体
        if (this.nearWater) {
            if (hasNearbyLiquid(world, belowPos, true)) {
                return true;
            }
        }
        if (this.nearLava) {
            if (hasNearbyLiquid(world, belowPos, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyLiquid(Object world, Object blockPos, boolean waterOrLava) throws ReflectiveOperationException {
        for (Object direction : HORIZON_DIRECTIONS) {
            Object relativePos = Reflections.method$BlockPos$relative.invoke(blockPos, direction);
            if (waterOrLava) {
                // water
                Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, relativePos);
                if (Reflections.method$BlockStateBase$getBlock.invoke(blockState) == Reflections.instance$Blocks$ICE) {
                    return true;
                }
                Object fluidState = Reflections.method$Level$getFluidState.invoke(world, relativePos);
                Object fluidType = Reflections.method$FluidState$getType.invoke(fluidState);
                if (WATER.contains(fluidType)) {
                    return true;
                }
            } else {
                // lava
                Object fluidState = Reflections.method$Level$getFluidState.invoke(world, relativePos);
                Object fluidType = Reflections.method$FluidState$getType.invoke(fluidState);
                if (LAVA.contains(fluidType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, false);
            Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.sugar_cane.missing_age");
            int maxHeight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-height", 3), "max-height");
            List<String> nearbyLiquids = MiscUtils.getAsStringList(arguments.getOrDefault("required-adjacent-liquids", List.of()));
            boolean nearWater = nearbyLiquids.contains("water");
            boolean nearLava = nearbyLiquids.contains("lava");
            return new SugarCaneBlockBehavior(block, tuple.left(), tuple.mid(), tuple.right(), ageProperty, maxHeight, nearWater, nearLava,
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1), "grow-speed"));
        }
    }
}
