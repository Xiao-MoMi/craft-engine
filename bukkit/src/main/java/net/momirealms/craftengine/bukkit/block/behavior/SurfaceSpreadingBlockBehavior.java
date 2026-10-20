package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.FluidTagsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SnowLayerBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LightEngineProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class SurfaceSpreadingBlockBehavior extends BukkitBlockBehavior implements RandomTickBlock {
    public static final BlockBehaviorFactory<SurfaceSpreadingBlockBehavior> FACTORY = new Factory();
    public final int lightRequirement;
    public final int maxLightRequirement;
    public final LazyReference<Object> baseBlock;
    public final Property<Boolean> snowyProperty;

    private SurfaceSpreadingBlockBehavior(BlockDefinition blockDefinition, int lightRequirement, int maxLightRequirement, String baseBlock, @Nullable Property<Boolean> snowyProperty) {
        super(blockDefinition);
        this.lightRequirement = lightRequirement;
        this.maxLightRequirement = maxLightRequirement;
        this.snowyProperty = snowyProperty;
        this.baseBlock = LazyReference.untilNotNull(() -> Objects.requireNonNull(BukkitBlockManager.instance().createBlockState(baseBlock)).minecraftState());
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (!canBeGrass(state, level, pos)) {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, this.baseBlock.get(), 3);
            return;
        }
        int brightness = LevelReaderProxy.INSTANCE.getMaxLocalRawBrightness(level, BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.UP));
        if (brightness < this.lightRequirement || brightness > this.maxLightRequirement) {
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
                        BlocksProxy.SNOW
                );
                newState = newState.with(this.snowyProperty, hasSnow);
            }
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState.customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
        }
    }

    private static boolean canBeGrass(Object state, Object level, Object pos) {
        Object blockPos = BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.UP);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, blockPos);
        if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, BlocksProxy.SNOW) && ((Integer) StateHolderProxy.INSTANCE.getValue(blockState, SnowLayerBlockProxy.LAYERS)) == 1) {
            return true;
        } else if (FluidStateProxy.INSTANCE.getAmount(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == 8) {
            return false;
        } else {
            if (VersionHelper.isOrAbove1_21_2) {
                return LightEngineProxy.INSTANCE.getLightDampeningInto(
                        state, blockState, DirectionProxy.UP,
                        BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightDampening$1(blockState)
                ) < 15;
            } else {
                return LightEngineProxy.INSTANCE.getLightDampeningInto(
                        level, state, pos, blockState, blockPos, DirectionProxy.UP,
                        BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightBlock(blockState, level, pos)
                ) < 15;
            }
        }
    }

    private static boolean canPropagate(Object state, Object level, Object pos) {
        Object blockPos = BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.UP);
        return canBeGrass(state, level, pos) && !FluidStateProxy.INSTANCE.is$1(BlockGetterProxy.INSTANCE.getFluidState(level, blockPos), FluidTagsProxy.WATER);
    }

    private static class Factory implements BlockBehaviorFactory<SurfaceSpreadingBlockBehavior> {
        private static final String[] LIGHT_REQUIREMENT = ConfigKeys.of("light_requirement|required_light");
        private static final String[] MAX_LIGHT_REQUIREMENT = ConfigKeys.of("max_light_requirement");
        private static final String[] BASE_BLOCK = ConfigKeys.of("base_block");

        @Override
        public SurfaceSpreadingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SurfaceSpreadingBlockBehavior(
                    block,
                    section.getInt(LIGHT_REQUIREMENT, 0),
                    section.getInt(MAX_LIGHT_REQUIREMENT, 15),
                    section.getString(BASE_BLOCK, "minecraft:dirt"),
                    BlockBehaviorFactory.getOptionalProperty(block, "snowy", Boolean.class)
            );
        }
    }
}
