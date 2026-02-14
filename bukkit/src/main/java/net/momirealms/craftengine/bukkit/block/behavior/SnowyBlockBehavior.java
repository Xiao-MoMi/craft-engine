package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.BlockTagsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

import java.util.Map;
import java.util.concurrent.Callable;

public class SnowyBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<SnowyBlockBehavior> FACTORY = new Factory();
    private final BooleanProperty snowyProperty;

    public SnowyBlockBehavior(CustomBlock customBlock, BooleanProperty snowyProperty) {
        super(customBlock);
        this.snowyProperty = snowyProperty;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (args[updateShape$direction] != DirectionProxy.UP) return superMethod.call();
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null || state.isEmpty()) return superMethod.call();
        ImmutableBlockState newState = state.with(this.snowyProperty, isSnowySetting(args[updateShape$neighborState]));
        return newState.customBlockState().literalObject();
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos().above()));
        return state.with(this.snowyProperty, isSnowySetting(blockState));
    }

    private static boolean isSnowySetting(Object state) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(state, BlockTagsProxy.SNOW);
    }

    private static class Factory implements BlockBehaviorFactory<SnowyBlockBehavior> {

        @Override
        public SnowyBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            BooleanProperty snowyProperty = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("snowy"), "warning.config.block.behavior.snowy.missing_snowy");
            return new SnowyBlockBehavior(block, snowyProperty);
        }
    }
}
