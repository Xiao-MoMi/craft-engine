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
import java.util.Locale;
import java.util.StringTokenizer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VerticalCropBlockBehavior extends BukkitBlockBehavior {
  public static final Factory FACTORY = new Factory();
  private static final ObjectArrayList<Object> WATER = ObjectArrayList.of(MFluids.WATER, MFluids.FLOWING_WATER);
  private static final ObjectArrayList<Object> LAVA = ObjectArrayList.of(MFluids.LAVA, MFluids.FLOWING_LAVA);

  private final int maxHeight;
  private final IntegerProperty ageProperty;
  private final BlockPos[] liquidPositions;
  private final boolean requireWater;
  private final boolean requireLava;
  private final boolean stopOverwaterGrowing;
  private final boolean allowAirGrow;
  private final boolean allowWaterGrow;
  private final boolean direction;
  private final float growSpeed;

  public VerticalCropBlockBehavior(CustomBlock customBlock,Property<Integer> ageProperty,int maxHeight,float growSpeed,boolean direction,BlockPos[] liquidPositions,boolean requireWater,boolean requireLava,boolean stopOverwaterGrowing,boolean allowAirGrow,boolean allowWaterGrow) {
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

  private boolean canGrow(Object level, BlockPos currentPos) {
    if (this.liquidPositions.length == 0)
      return true;
    for (BlockPos offset : this.liquidPositions) {
      Object checkPos = LocationUtils.toBlockPos(
          currentPos.x() + offset.x(),
          currentPos.y() + offset.y(),
          currentPos.z() + offset.z());
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
    Optional<ImmutableBlockState> optionalState = BlockStateUtils.getOptionalCustomBlockState(blockState);
    if (optionalState.isEmpty()) {
      return;
    }
    ImmutableBlockState currentState = optionalState.get();
    BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
    if (!canGrow(level, currentPos)) {
      return;
    }
    Object targetPos = direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
    Object directionFluid = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, targetPos);
    boolean canGrowAir = allowAirGrow
        && FastNMS.INSTANCE.method$FluidState$getType(directionFluid) == MFluids.EMPTY
        && FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, targetPos) == MBlocks.AIR$defaultState;
    boolean canGrowWater = allowWaterGrow
        && WATER.contains(FastNMS.INSTANCE.method$FluidState$getType(directionFluid))
        && !stopOverwaterGrowing;
    if (!canGrowAir && !canGrowWater) {
      return;
    }
    int height = 1;
    while (true) {
      Object pos = LocationUtils.toBlockPos(
          currentPos.x(), direction ? currentPos.y() - height : currentPos.y() + height, currentPos.z());
      Optional<ImmutableBlockState> nextOpt = BlockStateUtils
          .getOptionalCustomBlockState(FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, pos));
      if (nextOpt.isPresent() && nextOpt.get().owner().value() == customBlock) {
        height++;
        continue;
      }
      break;
    }
    if (height >= maxHeight) {
      return;
    }
    int age = currentState.get(ageProperty);
    if (age >= ageProperty.max || RandomUtils.generateRandomFloat(0, 1) < growSpeed) {
      if (VersionHelper.isOrAbove1_21_5()) {
        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent
            .invoke(null, level, targetPos, customBlock.defaultState().customBlockState().handle(),
                UpdateOption.UPDATE_ALL.flags());
      } else {
        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent
            .invoke(null, level, targetPos, customBlock.defaultState().customBlockState().handle());
      }
      FastNMS.INSTANCE.method$LevelWriter$setBlock(
          level, blockPos,
          currentState.with(ageProperty, ageProperty.min).customBlockState().handle(),
          UpdateOption.UPDATE_NONE.flags());
      return;
    }
    if (RandomUtils.generateRandomFloat(0, 1) < growSpeed) {
      FastNMS.INSTANCE.method$LevelWriter$setBlock(
          level, blockPos,
          currentState.with(ageProperty, age + 1).customBlockState().handle(),
          UpdateOption.UPDATE_NONE.flags());
    }
  }

  public static class Factory implements BlockBehaviorFactory {
    @SuppressWarnings("unchecked")
    @Override
    public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
      Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.vertical_crop.missing_age");
      int maxHeight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-height", 3), "max-height");
      boolean direction = "up".equals(arguments.getOrDefault("direction", "up").toString().toLowerCase(Locale.ROOT));
      List<String> requiredLiquids = MiscUtils.getAsStringList(arguments.getOrDefault("required-liquids", ObjectArrayList.of()));
      boolean reqWater = requiredLiquids.contains("water");
      boolean reqLava = requiredLiquids.contains("lava");
      List<String> posStrings = MiscUtils.getAsStringList(arguments.getOrDefault("liquids-pos", ObjectArrayList.of()));
      BlockPos[] liquidPositions = new BlockPos[posStrings.size()];
      for (int i = 0; i < posStrings.size(); i++) {
        StringTokenizer tokenizer = new StringTokenizer(posStrings.get(i), ",");
        int x = Integer.parseInt(tokenizer.nextToken());
        int y = Integer.parseInt(tokenizer.nextToken());
        int z = Integer.parseInt(tokenizer.nextToken());
        liquidPositions[i] = new BlockPos(x, y, z);
      }
      boolean stopOver = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stop-overwater-growing", false),"stop-overwater-growing");
      List<String> growTypes = MiscUtils.getAsStringList(arguments.getOrDefault("grow-types", ObjectArrayList.of("air")));
      boolean allowAir = growTypes.contains("air");
      boolean allowWater = growTypes.contains("water");
      float growSpeed = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1),"grow-speed");
      return new VerticalCropBlockBehavior(block,ageProperty,maxHeight,growSpeed,direction,liquidPositions,reqWater,reqLava,stopOver,allowAir,allowWater);
    }
  }
}
