package net.momirealms.craftengine.bukkit.block.behavior;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;

import java.util.*;
import java.util.concurrent.Callable;

public class VerticalCropBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final int maxHeight;
    private final IntegerProperty ageProperty;
    private final List<BlockPos> liquidPositions;
    private final List<Object> requiredLiquids;
    private final boolean growInAir;
    private final boolean growInLiquid;
    private final boolean verticalDirection;
    private final float growSpeed;

    public VerticalCropBlockBehavior(
            CustomBlock customBlock,
            IntegerProperty ageProperty,
            int maxHeight,
            float growSpeed,
            boolean verticalDirection,
            List<BlockPos> liquidPositions,
            List<Object> requiredLiquids,
            boolean growInAir,
            boolean growInLiquid
    ) {
        super(customBlock);
        this.ageProperty = ageProperty;
        this.maxHeight = maxHeight;
        this.growSpeed = growSpeed;
        this.verticalDirection = verticalDirection;
        this.liquidPositions = liquidPositions;
        this.requiredLiquids = requiredLiquids;
        this.growInAir = growInAir;
        this.growInLiquid = growInLiquid;
    }

    private boolean canGrow(Object level, BlockPos currentPos, Object targetPos) {
        Object targetBlockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, targetPos);
        Object targetFluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, targetPos);
        Object targetFluidType = FastNMS.INSTANCE.method$FluidState$getType(targetFluidState);
        boolean canGrowInAir = this.growInAir && targetFluidType == MFluids.EMPTY && targetBlockState == MBlocks.AIR$defaultState;
        boolean canGrowInWater = this.growInLiquid && this.requiredLiquids.contains(targetFluidType);
        if (!canGrowInAir && !canGrowInWater) return false;
        if (this.liquidPositions.isEmpty()) return true;
        for (BlockPos offset : this.liquidPositions) {
            Object checkPos = LocationUtils.toBlockPos(currentPos.x() + offset.x(), currentPos.y() + offset.y(), currentPos.z() + offset.z());
            Object fluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, checkPos);
            Object fluidType = FastNMS.INSTANCE.method$FluidState$getType(fluidState);
            if (this.requiredLiquids.contains(fluidType)) return true;
        }
        return false;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        Optional<ImmutableBlockState> optionalCurrentState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCurrentState.isEmpty()) return;
        ImmutableBlockState currentState = optionalCurrentState.get();
        BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
        Object targetPos = this.verticalDirection ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
        if (!canGrow(level, currentPos, targetPos)) return;
        int currentHeight = 1;
        while (true) {
            Object nextPos = LocationUtils.toBlockPos(currentPos.x(), this.verticalDirection ? currentPos.y() - currentHeight : currentPos.y() + currentHeight, currentPos.z());
            Object nextState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, nextPos);
            Optional<ImmutableBlockState> optionalBelowCustomState = BlockStateUtils.getOptionalCustomBlockState(nextState);
            if (optionalBelowCustomState.isPresent() && optionalBelowCustomState.get().owner().value() == super.customBlock) {
                currentHeight++;
            } else {
                break;
            }
        }
        if (currentHeight >= this.maxHeight) return;
        int age = currentState.get(this.ageProperty);
        if (age >= this.ageProperty.max || RandomUtils.generateRandomFloat(0, 1) < growSpeed) {
            if (VersionHelper.isOrAbove1_21_5()) {
                CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, targetPos, customBlock.defaultState().customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
            } else {
                CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, targetPos, customBlock.defaultState().customBlockState().handle());
            }
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, this.ageProperty.min).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
            return;
        }
        if (RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, age + 1).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
        }
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            IntegerProperty ageProperty = (IntegerProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.vertical_crop.missing_age");
            int maxHeight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-height", 3), "max-height");
            boolean verticalDirection = "up".equalsIgnoreCase(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.getOrDefault("direction", "up"), "direction"));
            List<Object> requiredLiquids = MiscUtils.getAsStringList(arguments.getOrDefault("required-liquids", List.of())).stream().map(MFluids::getById).collect(ObjectArrayList.toList());
            List<BlockPos> liquidPositions = MiscUtils.getAsStringList(arguments.getOrDefault("liquids-pos", List.of())).stream().map(s -> {
                StringTokenizer tokenizer = new StringTokenizer(s, ",");
                int x = Integer.parseInt(tokenizer.nextToken());
                int y = Integer.parseInt(tokenizer.nextToken());
                int z = Integer.parseInt(tokenizer.nextToken());
                return new BlockPos(x, y, z);
            }).collect(ObjectArrayList.toList());
            boolean growInAir = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("grow-in-air", true), "grow-in-air");
            boolean growInLiquid = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("grow-in-liquid", false), "grow-in-liquid");
            float growSpeed = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1),"grow-speed");
            return new VerticalCropBlockBehavior(block, ageProperty, maxHeight, growSpeed, verticalDirection, liquidPositions, requiredLiquids, growInAir, growInLiquid);
        }
    }
}
