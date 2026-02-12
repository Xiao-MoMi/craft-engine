package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MTagKeys;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

public class SurfaceSpreadingBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<SurfaceSpreadingBlockBehavior> FACTORY = new Factory();
    private final int requiredLight;
    private final LazyReference<Object> baseBlock;
    private final Property<Boolean> snowyProperty;

    public SurfaceSpreadingBlockBehavior(CustomBlock customBlock, int requiredLight, String baseBlock, @Nullable Property<Boolean> snowyProperty) {
        super(customBlock);
        this.requiredLight = requiredLight;
        this.snowyProperty = snowyProperty;
        this.baseBlock = LazyReference.lazyReference(() -> Objects.requireNonNull(BukkitBlockManager.instance().createBlockState(baseBlock)).literalObject());
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (!canBeGrass(state, level, pos)) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, this.baseBlock.get(), 3);
            return;
        }
        if (FastNMS.INSTANCE.method$LevelReader$getMaxLocalRawBrightness(level, FastNMS.INSTANCE.method$BlockPos$relative(pos, CoreReflections.instance$Direction$UP)) < this.requiredLight) return;

        for (int i = 0; i < 4; i++) {
            Object blockPos = FastNMS.INSTANCE.method$BlockPos$offset(
                pos, 
                RandomUtils.generateRandomInt(-1, 2), 
                RandomUtils.generateRandomInt(-3, 2), 
                RandomUtils.generateRandomInt(-1, 2)
            );
            
            if (FastNMS.INSTANCE.method$BlockStateBase$isBlock(
                FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos), 
                FastNMS.INSTANCE.method$BlockState$getBlock(this.baseBlock.get())
            ) && canPropagate(state, level, blockPos)) {
                
                ImmutableBlockState newState = this.block().defaultState();
                
                if (this.snowyProperty != null) {
                    boolean hasSnow = FastNMS.INSTANCE.method$BlockStateBase$isBlock(
                        FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, 
                            FastNMS.INSTANCE.method$BlockPos$relative(blockPos, CoreReflections.instance$Direction$UP)), 
                        MBlocks.SNOW
                    );
                    newState = newState.with(this.snowyProperty, hasSnow);
                }
                
                FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, newState.customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
            }
        }
    }

    private static boolean canBeGrass(Object state, Object level, Object pos) {
        Object blockPos = FastNMS.INSTANCE.method$BlockPos$relative(pos, CoreReflections.instance$Direction$UP);
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos);
        if (FastNMS.INSTANCE.method$BlockStateBase$isBlock(blockState, MBlocks.SNOW) && ((Integer) FastNMS.INSTANCE.method$StateHolder$getValue(blockState, CoreReflections.instance$SnowLayerBlock$LAYERS)) == 1) return true;
        else if (FastNMS.INSTANCE.field$FluidState$amount(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(blockState)) == 8) return false;
        else {
            return FastNMS.INSTANCE.method$LightEngine$getLightBlockInto(
                    VersionHelper.isOrAbove1_21_2() ? null : level,
                    state,
                    VersionHelper.isOrAbove1_21_2() ? null : pos,
                    blockState,
                    VersionHelper.isOrAbove1_21_2() ? null : blockPos,
                    CoreReflections.instance$Direction$UP,
                    FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getLightBlock(
                            blockState,
                            VersionHelper.isOrAbove1_21_2() ? null : level,
                            VersionHelper.isOrAbove1_21_2() ? null : pos
                    )
            ) < 15;
        }
    }

    private static boolean canPropagate(Object state, Object level, Object pos) {
        Object blockPos = FastNMS.INSTANCE.method$BlockPos$relative(pos, CoreReflections.instance$Direction$UP);
        return canBeGrass(state, level, pos) && !FastNMS.INSTANCE.method$FluidState$is(FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, blockPos), MTagKeys.Fluid$WATER);
    }

    private static class Factory implements BlockBehaviorFactory<SurfaceSpreadingBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public SurfaceSpreadingBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            int requiredLight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("required-light", 9), "required-light");
            String baseBlock = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.getOrDefault("base-block", "minecraft:dirt"), "warning.config.block.behavior.surface_spreading.missing_base_block");
            return new SurfaceSpreadingBlockBehavior(block, requiredLight, baseBlock, (Property<Boolean>) block.getProperty("snowy"));
        }
    }
}
