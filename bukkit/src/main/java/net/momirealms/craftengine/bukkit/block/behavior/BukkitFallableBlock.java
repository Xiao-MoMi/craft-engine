package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.FallableBlock;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourcesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;

public interface BukkitFallableBlock extends FallableBlock {

    @Override
    default Object getFallDamageSource(Object thisBlock, Object[] args) {
        return DamageSourcesProxy.INSTANCE.fallingBlock(EntityProxy.INSTANCE.damageSources(args[0]), args[0]);
    }
}
