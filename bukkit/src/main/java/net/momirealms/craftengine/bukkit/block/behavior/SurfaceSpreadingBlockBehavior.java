package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
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
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SnowLayerBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LightEngineProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
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
            LevelWriterProxy.INSTANCE.setBlock(level, pos, this.baseBlock.get(), 3);
            return;
        }
        if (LevelReaderProxy.INSTANCE.getMaxLocalRawBrightness(level, BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.UP)) < this.requiredLight) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            Object blockPos = BlockPosProxy.INSTANCE.offset(
                    pos,
                    RandomUtils.generateRandomInt(-1, 2),
                    RandomUtils.generateRandomInt(-3, 2),
                    RandomUtils.generateRandomInt(-1, 2)
            );
            boolean isTargetBlock = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(
                    BlockGetterProxy.INSTANCE.getBlockState(level, blockPos),
                    BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(this.baseBlock.get())
            );
            if (!isTargetBlock || !canPropagate(state, level, blockPos)) continue;
            ImmutableBlockState newState = this.block().defaultState();
            if (this.snowyProperty != null) {
                boolean hasSnow = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(
                        BlockGetterProxy.INSTANCE.getBlockState(
                                level, BlockPosProxy.INSTANCE.relative(blockPos, DirectionProxy.UP)
                        ),
                        MBlocks.SNOW
                );
                newState = newState.with(this.snowyProperty, hasSnow);
            }
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState.customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        }
    }

    private static boolean canBeGrass(Object state, Object level, Object pos) {
        Object blockPos = BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.UP);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, blockPos);
        if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, MBlocks.SNOW) && ((Integer) StateHolderProxy.INSTANCE.getValue(blockState, SnowLayerBlockProxy.INSTANCE.getLayersProperty())) == 1) {
            return true;
        } else if (FluidStateProxy.INSTANCE.getAmount(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == 8) {
            return false;
        } else {
            if (VersionHelper.isOrAbove1_21_2()) {
                return LightEngineProxy.INSTANCE.getLightBlockInto(
                        state, blockState, DirectionProxy.UP,
                        BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightBlock$1(blockState)
                ) < 15;
            } else {
                return LightEngineProxy.INSTANCE.getLightBlockInto(
                        level, state, pos, blockState, blockPos, DirectionProxy.UP,
                        BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightBlock(blockState, level, pos)
                ) < 15;
            }
        }
    }

    private static boolean canPropagate(Object state, Object level, Object pos) {
        Object blockPos = BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.UP);
        return canBeGrass(state, level, pos) && !FluidStateProxy.INSTANCE.is(BlockGetterProxy.INSTANCE.getFluidState(level, blockPos), MTagKeys.Fluid$WATER);
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
