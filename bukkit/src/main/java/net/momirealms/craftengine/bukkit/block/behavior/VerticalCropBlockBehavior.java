package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class VerticalCropBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final int maxHeight;
    private final IntegerProperty ageProperty;
    private static final List<Object> WATER = List.of(MFluids.WATER, MFluids.FLOWING_WATER);
    private final BooleanProperty waterloggedProperty;
    private final float growSpeed;
    private final boolean direction;
    private final boolean waterlogged;

    public VerticalCropBlockBehavior(CustomBlock customBlock, Property<Integer> ageProperty, Property<Boolean> waterloggedProperty,
                                     int maxHeight, float growSpeed, boolean direction, boolean waterlogged) {
        super(customBlock);
        this.maxHeight = maxHeight;
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.direction = direction;
        this.waterloggedProperty = (BooleanProperty) waterloggedProperty;
        this.waterlogged = waterlogged;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        Optional<ImmutableBlockState> optionalCurrentState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCurrentState.isEmpty()) {
            return;
        }
        ImmutableBlockState currentState = optionalCurrentState.get();
        Object directionBlock = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, (this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos)));
        Object directionFluid = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, (this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos)));
        // above block is empty
        Boolean isWaterlogged = this.waterlogged && WATER.contains(FastNMS.INSTANCE.method$FluidState$getType(directionFluid));
        if (directionBlock == MBlocks.AIR$defaultState || isWaterlogged) {
            int currentHeight = 1;
            BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
            while (true) {
                Object nextPos = LocationUtils.toBlockPos(currentPos.x(), this.direction ? currentPos.y() - currentHeight : currentPos.y() + currentHeight, currentPos.z());
                Object nextState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, nextPos);
                Optional<ImmutableBlockState> optionalBelowCustomState = BlockStateUtils.getOptionalCustomBlockState(nextState);
                if (optionalBelowCustomState.isPresent() && optionalBelowCustomState.get().owner().value() == super.customBlock) {
                    currentHeight++;
                } else {
                    break;
                }
            }
            if (currentHeight < this.maxHeight) {
                int age = currentState.get(ageProperty);
                if (age >= this.ageProperty.max || RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    Object nextPos = this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
                    if (VersionHelper.isOrAbove1_21_5()) {
                        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, nextPos, super.customBlock.defaultState().customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
                    } else {
                        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, nextPos, super.customBlock.defaultState().customBlockState().handle());
                    }
                    if(!waterlogged)FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, this.ageProperty.min).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
                    if(waterlogged)FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, this.ageProperty.min).with(this.waterloggedProperty, true).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
                } else if (RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    if(!waterlogged)FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, age + 1).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
                    if(waterlogged)FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, age + 1).with(this.waterloggedProperty, true).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
                }
            }
        }
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.vertical_crop.missing_age");
            int maxHeight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-height", 3), "max-height");
            boolean direction = arguments.getOrDefault("direction", "up").toString().equalsIgnoreCase("up");
            boolean waterlogged = arguments.getOrDefault("waterlogged", false).toString().equalsIgnoreCase("true");
            Property<Boolean> waterloggedProperty = null;
            if (waterlogged) {
                waterloggedProperty = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("waterlogged"), "warning.config.block.behavior.vertical_crop.missing_waterlogged");
            }
            return new VerticalCropBlockBehavior(block, ageProperty, waterloggedProperty, maxHeight,
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1), "grow-speed"), direction, waterlogged);
        }
    }
}
