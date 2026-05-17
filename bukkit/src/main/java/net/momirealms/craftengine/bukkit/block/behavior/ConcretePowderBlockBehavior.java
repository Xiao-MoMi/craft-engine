package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockStateProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockStatesProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MutableBlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;
import org.jetbrains.annotations.Nullable;

public final class ConcretePowderBlockBehavior extends BukkitBlockBehavior implements BukkitFallableBlock {
    public static final BlockBehaviorFactory<ConcretePowderBlockBehavior> FACTORY = new Factory();
    public final LazyReference<@Nullable ImmutableBlockState> targetBlock;

    private ConcretePowderBlockBehavior(BlockDefinition block, String targetBlock) {
        super(block);
        this.targetBlock = LazyReference.lazyReference(() -> BlockStateParser.deserialize(targetBlock));
    }

    public Object getDefaultBlockState() {
        ImmutableBlockState state = this.targetBlock.get();
        return state != null ? state.customBlockState().minecraftState() : BlocksProxy.STONE$defaultState;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().minecraftWorld();
        Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());
        Object previousState = BlockGetterProxy.INSTANCE.getBlockState(level, blockPos);
        if (!shouldSolidify(level, blockPos, previousState)) {
            return super.updateStateForPlacement(context, state);
        } else {
            BlockState craftBlockState = (BlockState) CraftBlockStatesProxy.INSTANCE.getBlockState(level, blockPos);
            craftBlockState.setBlockData(BlockStateUtils.fromBlockData(getDefaultBlockState()));
            BlockFormEvent event = new BlockFormEvent(craftBlockState.getBlock(), craftBlockState);
            if (!EventUtils.fireAndCheckCancel(event)) {
                return this.targetBlock.get();
            } else {
                return super.updateStateForPlacement(context, state);
            }
        }
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) {
        Object world = args[0];
        Object blockPos = args[1];
        Object replaceableState = args[3];
        if (shouldSolidify(world, blockPos, replaceableState)) {
            CraftEventFactoryProxy.INSTANCE.handleBlockFormEvent(world, blockPos, getDefaultBlockState(), UpdateFlags.UPDATE_ALL);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object level = args[updateShape$level];
        Object pos = args[updateShape$blockPos];
        if (touchesLiquid(level, pos)) {
            if (!LevelProxy.CLASS.isInstance(level)) {
                return getDefaultBlockState();
            } else {
                BlockState craftBlockState = (BlockState) CraftBlockStatesProxy.INSTANCE.getBlockState(level, pos);
                craftBlockState.setBlockData(BlockStateUtils.fromBlockData(getDefaultBlockState()));
                BlockFormEvent event = new BlockFormEvent(craftBlockState.getBlock(), craftBlockState);
                if (!EventUtils.fireAndCheckCancel(event)) {
                    return CraftBlockStateProxy.INSTANCE.getHandle(craftBlockState);
                }
            }
        }
        return args[0];
    }

    private static boolean shouldSolidify(Object level, Object blockPos, Object blockState) {
        return canSolidify(blockState) || touchesLiquid(level, blockPos);
    }

    private static boolean canSolidify(Object state) {
        Object fluidState = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(state);
        if (fluidState == null) return false;
        Object fluidType = FluidStateProxy.INSTANCE.getType(fluidState);
        return fluidType == FluidsProxy.WATER || fluidType == FluidsProxy.FLOWING_WATER;
    }

    private static boolean touchesLiquid(Object level, Object pos) {
        boolean flag = false;
        Object mutablePos = BlockPosProxy.INSTANCE.mutable(pos);
        int j = Direction.values().length;
        for (int k = 0; k < j; k++) {
            Object direction = DirectionProxy.VALUES[k];
            Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, mutablePos);
            if (direction != DirectionProxy.DOWN || canSolidify(blockState)) {
                MutableBlockPosProxy.INSTANCE.setWithOffset(mutablePos, pos, direction);
                blockState = BlockGetterProxy.INSTANCE.getBlockState(level, mutablePos);
                if (canSolidify(blockState) && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(blockState, level, pos, DirectionProxy.INSTANCE.getOpposite(direction), SupportTypeProxy.FULL)) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    private static class Factory implements BlockBehaviorFactory<ConcretePowderBlockBehavior> {
        private static final String[] SOLID_BLOCK = new String[] {"solid_block", "solid-block"};

        @Override
        public ConcretePowderBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new ConcretePowderBlockBehavior(
                    block,
                    section.getNonNullString(SOLID_BLOCK)
            );
        }
    }
}
