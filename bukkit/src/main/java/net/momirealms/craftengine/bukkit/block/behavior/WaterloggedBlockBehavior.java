package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import java.util.Optional;

public class WaterloggedBlockBehavior extends BukkitBlockBehavior implements BukkitSimpleWaterloggedBlock {
    protected final Property<Boolean> waterloggedProperty;

    public WaterloggedBlockBehavior(BlockDefinition blockDefinition, Property<Boolean> waterloggedProperty) {
        super(blockDefinition);
        this.waterloggedProperty = waterloggedProperty;
    }

    private static final int pickupBlock$world = VersionHelper.isOrAbove1_20_2 ? 1 : 0;
    private static final int pickupBlock$pos = VersionHelper.isOrAbove1_20_2 ? 2 : 1;
    private static final int pickupBlock$blockState = VersionHelper.isOrAbove1_20_2 ? 3 : 2;

    @Override
    public Object pickupBlock(Object thisBlock, Object[] args) {
        Object blockState = args[pickupBlock$blockState];
        Object world = args[pickupBlock$world];
        Object pos = args[pickupBlock$pos];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return ItemStackProxy.EMPTY;
        ImmutableBlockState immutableBlockState = optionalCustomState.get();
        if (immutableBlockState.get(this.waterloggedProperty)) {
            LevelWriterProxy.INSTANCE.setBlock(world, pos, immutableBlockState.with(this.waterloggedProperty, false).customBlockState().minecraftState(), 3);
            return ItemStackProxy.INSTANCE.newInstance(ItemsProxy.WATER_BUCKET, 1);
        }
        return ItemStackProxy.EMPTY;
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args) {
        Object blockState = args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        ImmutableBlockState immutableBlockState = optionalCustomState.get();
        Object fluidType = FluidStateProxy.INSTANCE.getType(args[3]);
        if (!immutableBlockState.get(this.waterloggedProperty) && fluidType == FluidsProxy.WATER) {
            LevelWriterProxy.INSTANCE.setBlock(args[0], args[1], immutableBlockState.with(this.waterloggedProperty, true).customBlockState().minecraftState(), 3);
            LevelAccessorProxy.INSTANCE.scheduleTick$1(args[0], args[1], fluidType, 5);
            return true;
        }
        return false;
    }

    private static final int canPlaceLiquid$liquid = VersionHelper.isOrAbove1_20_2 ? 4 : 3;

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args) {
        return args[canPlaceLiquid$liquid] == FluidsProxy.WATER;
    }
}
