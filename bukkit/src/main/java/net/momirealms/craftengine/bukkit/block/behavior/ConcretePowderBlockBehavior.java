package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.LiquidSolidification;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockStateProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockStatesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;
import org.jetbrains.annotations.Nullable;

public final class ConcretePowderBlockBehavior extends BukkitBlockBehavior implements BukkitFallableBlock, LiquidSolidifiableBlock {
    public static final BlockBehaviorFactory<ConcretePowderBlockBehavior> FACTORY = new Factory();
    public final LazyReference<@Nullable ImmutableBlockState> targetBlock;

    private ConcretePowderBlockBehavior(BlockDefinition block, String targetBlock) {
        super(block);
        this.targetBlock = LazyReference.untilNotNull(() -> BlockStateParser.deserialize(targetBlock));
    }

    @Override
    public boolean canSolidifyWith(Object fluidState) {
        Object fluidType = FluidStateProxy.INSTANCE.getType(fluidState);
        return fluidType == FluidsProxy.WATER || fluidType == FluidsProxy.FLOWING_WATER;
    }

    @Override
    public @Nullable ImmutableBlockState solidifiedState(ImmutableBlockState sourceState) {
        return this.targetBlock.get();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().minecraftWorld();
        Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());
        Object previousState = BlockGetterProxy.INSTANCE.getBlockState(level, blockPos);
        ImmutableBlockState targetState = LiquidSolidification.solidifiedStateAt(this, state, level, blockPos, previousState);
        if (targetState == null) {
            return super.updateStateForPlacement(context, state);
        } else {
            BlockState craftBlockState = (BlockState) CraftBlockStatesProxy.INSTANCE.getBlockState(level, blockPos);
            craftBlockState.setBlockData(BlockStateUtils.fromBlockData(targetState.customBlockState().minecraftState()));
            BlockFormEvent event = new BlockFormEvent(craftBlockState.getBlock(), craftBlockState);
            if (!EventUtils.fireAndCheckCancel(event)) {
                return targetState;
            } else {
                return super.updateStateForPlacement(context, state);
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object level = args[updateShape$level];
        Object pos = args[updateShape$blockPos];
        ImmutableBlockState sourceState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        ImmutableBlockState targetState = sourceState != null
                ? LiquidSolidification.touchingLiquid(this, sourceState, level, pos)
                : null;
        if (targetState != null) {
            if (!LevelProxy.CLASS.isInstance(level)) {
                return targetState.customBlockState().minecraftState();
            } else {
                BlockState craftBlockState = (BlockState) CraftBlockStatesProxy.INSTANCE.getBlockState(level, pos);
                craftBlockState.setBlockData(BlockStateUtils.fromBlockData(targetState.customBlockState().minecraftState()));
                BlockFormEvent event = new BlockFormEvent(craftBlockState.getBlock(), craftBlockState);
                if (!EventUtils.fireAndCheckCancel(event)) {
                    return CraftBlockStateProxy.INSTANCE.getHandle(craftBlockState);
                }
            }
        }
        return args[0];
    }

    private static class Factory implements BlockBehaviorFactory<ConcretePowderBlockBehavior> {
        private static final String[] SOLID_BLOCK = ConfigKeys.of("solid_block");

        @Override
        public ConcretePowderBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new ConcretePowderBlockBehavior(
                    block,
                    section.getNonNullString(SOLID_BLOCK)
            );
        }
    }
}
