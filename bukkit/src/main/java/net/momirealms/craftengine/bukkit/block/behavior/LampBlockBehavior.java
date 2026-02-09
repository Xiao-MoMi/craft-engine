package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public class LampBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<LampBlockBehavior> FACTORY = new Factory();
    private final Property<Boolean> litProperty;

    public LampBlockBehavior(CustomBlock block, Property<Boolean> litProperty) {
        super(block);
        this.litProperty = litProperty;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().serverWorld();
        state = state.with(this.litProperty, FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, LocationUtils.toBlockPos(context.getClickedPos())));
        return state;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        Object world = args[1];
        Object blockPos = args[2];
        ImmutableBlockState customState = optionalCustomState.get();
        if (customState.get(this.litProperty) && !FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(world, blockPos)) {
            if (CraftEventFactoryProxy.INSTANCE.callRedstoneChange(world, blockPos, 0, 15).getNewCurrent() != 15) {
                return;
            }
            FastNMS.INSTANCE.method$LevelWriter$setBlock(world, blockPos, customState.cycle(this.litProperty).customBlockState().literalObject(), 2);
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        Object world = args[1];
        Object blockPos = args[2];
        ImmutableBlockState customState = optionalCustomState.get();
        boolean lit = customState.get(this.litProperty);
        if (lit != FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(world, blockPos)) {
            if (lit) {
                LevelUtils.scheduleBlockTick(world, blockPos, thisBlock, 4);
            } else {
                if (CraftEventFactoryProxy.INSTANCE.callRedstoneChange(world, blockPos, 0, 15).getNewCurrent() != 15) {
                    return;
                }
                FastNMS.INSTANCE.method$LevelWriter$setBlock(world, blockPos, customState.cycle(this.litProperty).customBlockState().literalObject(), 2);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static class Factory implements BlockBehaviorFactory<LampBlockBehavior> {
        @Override
        public LampBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Boolean> lit = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("lit"), "warning.config.block.behavior.lamp.missing_lit");
            return new LampBlockBehavior(block, lit);
        }
    }
}
