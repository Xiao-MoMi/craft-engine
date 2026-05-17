package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.BlockTagsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

public final class SnowyBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<SnowyBlockBehavior> FACTORY = new Factory();
    public final Property<Boolean> snowyProperty;

    private SnowyBlockBehavior(BlockDefinition blockDefinition,
                               Property<Boolean> snowyProperty) {
        super(blockDefinition);
        this.snowyProperty = snowyProperty;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        if (args[updateShape$direction] != DirectionProxy.UP) return super.updateShape(thisBlock, args);
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null || state.isEmpty()) return super.updateShape(thisBlock, args);
        ImmutableBlockState newState = state.with(this.snowyProperty, isSnowySetting(args[updateShape$neighborState]));
        return newState.customBlockState().minecraftState();
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(context.getLevel().minecraftWorld(), LocationUtils.toBlockPos(context.getClickedPos().above()));
        return state.with(this.snowyProperty, isSnowySetting(blockState));
    }

    private static boolean isSnowySetting(Object state) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(state, BlockTagsProxy.SNOW);
    }

    private static class Factory implements BlockBehaviorFactory<SnowyBlockBehavior> {

        @Override
        public SnowyBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SnowyBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "snowy", Boolean.class)
            );
        }
    }
}
