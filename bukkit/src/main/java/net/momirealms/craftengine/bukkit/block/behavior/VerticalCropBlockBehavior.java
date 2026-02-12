package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class VerticalCropBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<VerticalCropBlockBehavior> FACTORY = new Factory();
    private final int maxHeight;
    private final IntegerProperty ageProperty;
    private final float growSpeed;
    private final boolean direction;

    public VerticalCropBlockBehavior(CustomBlock customBlock, Property<Integer> ageProperty,
                                     int maxHeight, float growSpeed, boolean direction) {
        super(customBlock);
        this.maxHeight = maxHeight;
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.direction = direction;
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
        // above block is empty
        if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, (this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos))) == MBlocks.AIR$defaultState) {
            int currentHeight = 1;
            BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
            for (;;) {
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
                        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, nextPos, super.customBlock.defaultState().customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
                    } else {
                        CraftBukkitReflections.method$CraftEventFactory$handleBlockGrowEvent.invoke(null, level, nextPos, super.customBlock.defaultState().customBlockState().literalObject());
                    }
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, this.ageProperty.min).customBlockState().literalObject(), UpdateOption.UPDATE_NONE.flags());
                } else if (RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, currentState.with(this.ageProperty, age + 1).customBlockState().literalObject(), UpdateOption.UPDATE_NONE.flags());
                }
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<VerticalCropBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public VerticalCropBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.vertical_crop.missing_age");
            int maxHeight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-height", 3), "max-height");
            boolean direction = arguments.getOrDefault("direction", "up").toString().equalsIgnoreCase("up");
            return new VerticalCropBlockBehavior(block, ageProperty, maxHeight,
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1), "grow-speed"), direction);
        }
    }
}
