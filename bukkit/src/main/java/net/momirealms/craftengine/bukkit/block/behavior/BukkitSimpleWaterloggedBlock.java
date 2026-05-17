package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.SimpleWaterloggedBlock;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

public interface BukkitSimpleWaterloggedBlock extends SimpleWaterloggedBlock {

    @Override
    default Object getPickupSound(Object thisBlock, Object[] args) {
        return FluidProxy.INSTANCE.getPickupSound(FluidsProxy.WATER);
    }

    @Override
    default boolean canPlaceLiquid(Object thisBlock, Object[] args) {
        return args[4] == FluidsProxy.WATER;
    }
}
