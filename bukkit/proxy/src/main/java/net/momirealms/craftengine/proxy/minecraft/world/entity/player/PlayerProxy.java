package net.momirealms.craftengine.proxy.minecraft.world.entity.player;

import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.player.Player")
public interface PlayerProxy extends LivingEntityProxy {
    PlayerProxy INSTANCE = ASMProxyFactory.create(PlayerProxy.class);
}
