package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.state.properties.DoubleBlockHalf;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.*;

import java.util.Map;
import java.util.concurrent.Callable;

public class DoubleHighBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Property<DoubleBlockHalf> halfProperty;

    public DoubleHighBlockBehavior(CustomBlock customBlock, Property<DoubleBlockHalf> halfProperty) {
        super(customBlock);
        this.halfProperty = halfProperty;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];

        ImmutableBlockState customState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (customState == null || customState.isEmpty()) return blockState;
        DoubleBlockHalf half = customState.get(this.halfProperty);
        if (half == null) return blockState;

        // 获取另一半
        Object anotherHalfPos = FastNMS.INSTANCE.method$BlockPos$relative(blockPos,
                half == DoubleBlockHalf.UPPER
                        ? CoreReflections.instance$Direction$DOWN
                        : CoreReflections.instance$Direction$UP
        );
        Object anotherHalfState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, anotherHalfPos);
        ImmutableBlockState anotherHalfCustomState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(anotherHalfState));
        if (anotherHalfCustomState != null && !anotherHalfCustomState.isEmpty()) return blockState;

        // 破坏
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
        WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
        world.playBlockSound(position, customState.settings().sounds().breakSound());
        FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId());
        return MBlocks.AIR$defaultState;
    }

    @Override
    public void setPlacedBy(BlockPlaceContext context, ImmutableBlockState state) {
        World level = context.getLevel();
        BlockPos anotherHalfPos = context.getClickedPos().relative(Direction.UP);
        BlockStateWrapper blockStateWrapper = state.with(this.halfProperty, DoubleBlockHalf.UPPER).customBlockState();
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level.serverWorld(), LocationUtils.toBlockPos(anotherHalfPos), blockStateWrapper.handle(), UpdateOption.Flags.UPDATE_CLIENTS);
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World world  = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.y() < context.getLevel().worldHeight().getMaxBuildHeight() && world.getBlockAt(pos.above()).canBeReplaced(context)) {
            return state.with(this.halfProperty, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<DoubleBlockHalf> half = (Property<DoubleBlockHalf>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("half"), "warning.config.block.behavior.double_high.missing_half");
            return new DoubleHighBlockBehavior(block, half);
        }
    }
}
