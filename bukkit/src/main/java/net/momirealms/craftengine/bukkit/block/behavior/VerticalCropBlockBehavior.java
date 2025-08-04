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
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.MiscUtils;
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
    private static final List<Object> LAVA = List.of(MFluids.LAVA, MFluids.FLOWING_LAVA);
    private final BlockPos[] liquidPositions;
    private final boolean requireWater;
    private final boolean requireLava;
    private final boolean stopOverwaterGrowing;
    private final boolean allowAirGrow;
    private final boolean allowWaterGrow;
    private final boolean direction;
    private final float growSpeed;

    public VerticalCropBlockBehavior(CustomBlock customBlock,
                                     Property<Integer> ageProperty,
                                     int maxHeight,
                                     float growSpeed,
                                     boolean direction,
                                     BlockPos[] liquidPositions,
                                     boolean requireWater,
                                     boolean requireLava,
                                     boolean stopOverwaterGrowing,
                                     boolean allowAirGrow,
                                     boolean allowWaterGrow) {
        super(customBlock);
        this.maxHeight = maxHeight;
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.direction = direction;
        this.liquidPositions = liquidPositions;
        this.requireWater = requireWater;
        this.requireLava = requireLava;
        this.stopOverwaterGrowing = stopOverwaterGrowing;
        this.allowAirGrow = allowAirGrow;
        this.allowWaterGrow = allowWaterGrow;
    }

    // método helper para verificar si cumple los requisitos de líquidos cercanos
    private boolean canGrow(Object level, BlockPos currentPos) {
        if (this.liquidPositions.length == 0) return true;
        for (BlockPos offset : this.liquidPositions) {
            Object checkPos = LocationUtils.toBlockPos(
                currentPos.x() + offset.x(),
                currentPos.y() + offset.y(),
                currentPos.z() + offset.z()
            );
            Object fs = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, checkPos);
            Object ft = FastNMS.INSTANCE.method$FluidState$getType(fs);
            if ((this.requireWater && WATER.contains(ft)) || (this.requireLava && LAVA.contains(ft))) {
                return true;
            }
        }
        return false;
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
        BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
        if (!canGrow(level, currentPos)) {
            return;
        }
        Object targetPos = this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
        Object directionBlock = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, targetPos);
        Object directionFluid = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, targetPos);
        boolean canGrowAir = this.allowAirGrow
            && directionBlock == MBlocks.AIR$defaultState
            && FastNMS.INSTANCE.method$FluidState$getType(directionFluid) == MFluids.EMPTY;
        boolean canGrowWater = this.allowWaterGrow
            && WATER.contains(FastNMS.INSTANCE.method$FluidState$getType(directionFluid))
            && !this.stopOverwaterGrowing;
        if (canGrowAir || canGrowWater) {
            int currentHeight = 1;
            while (true) {
                Object nextPos = LocationUtils.toBlockPos(
                    currentPos.x(),
                    this.direction ? currentPos.y() - currentHeight : currentPos.y() + currentHeight,
                    currentPos.z()
                );
                Object nextState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, nextPos);
                Optional<ImmutableBlockState> opt = BlockStateUtils.getOptionalCustomBlockState(nextState);
                if (opt.isPresent() && opt.get().owner().value() == super.customBlock) {
                    currentHeight++;
                } else {
                    break;
                }
            }
            if (currentHeight < this.maxHeight) {
                int age = currentState.get(ageProperty);
                Object newPos = targetPos;
                if (age >= this.ageProperty.max || RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    if (VersionHelper.isOrAbove1_21_5()) {
                        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent
                            .invoke(null, level, newPos,
                                    super.customBlock.defaultState().customBlockState().handle(),
                                    UpdateOption.UPDATE_ALL.flags());
                    } else {
                        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent
                            .invoke(null, level, newPos,
                                    super.customBlock.defaultState().customBlockState().handle());
                    }
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(
                        level, blockPos,
                        currentState.with(this.ageProperty, this.ageProperty.min)
                                     .customBlockState().handle(),
                        UpdateOption.UPDATE_NONE.flags()
                    );
                } else if (RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(
                        level, blockPos,
                        currentState.with(this.ageProperty, age + 1)
                                     .customBlockState().handle(),
                        UpdateOption.UPDATE_NONE.flags()
                    );
                }
            }
        }
    }

    public static class Factory implements BlockBehaviorFactory {
        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(
                block.getProperty("age"),
                "warning.config.block.behavior.vertical_crop.missing_age"
            );
            int maxHeight = ResourceConfigUtils.getAsInt(
                arguments.getOrDefault("max-height", 3),
                "max-height"
            );
            boolean direction = arguments.getOrDefault("direction", "up")
                .toString().equalsIgnoreCase("up");
            List<String> requiredLiquids = MiscUtils.getAsStringList(
                arguments.getOrDefault("required-liquids", List.of())
            );
            boolean reqWater = requiredLiquids.contains("water");
            boolean reqLava = requiredLiquids.contains("lava");
            List<String> posStrings = MiscUtils.getAsStringList(
                arguments.getOrDefault("liquids-pos", List.of())
            );
            BlockPos[] liquidPositions = new BlockPos[posStrings.size()];
            for (int i = 0; i < posStrings.size(); i++) {
                String[] split = posStrings.get(i).split(",");
                liquidPositions[i] = new BlockPos(
                    Integer.parseInt(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2])
                );
            }
            boolean stopOver = ResourceConfigUtils.getAsBoolean(
                arguments.getOrDefault("stop-overwater-growing", false),
                "stop-overwater-growing"
            );
            List<String> growTypes = MiscUtils.getAsStringList(
                arguments.getOrDefault("grow-types", List.of("air"))
            );
            boolean allowAir = growTypes.contains("air");
            boolean allowWater = growTypes.contains("water");
            return new VerticalCropBlockBehavior(
                block,
                ageProperty,
                maxHeight,
                ResourceConfigUtils.getAsFloat(
                    arguments.getOrDefault("grow-speed", 1),
                    "grow-speed"
                ),
                direction,
                liquidPositions,
                reqWater,
                reqLava,
                stopOver,
                allowAir,
                allowWater
            );
        }
    }
}
