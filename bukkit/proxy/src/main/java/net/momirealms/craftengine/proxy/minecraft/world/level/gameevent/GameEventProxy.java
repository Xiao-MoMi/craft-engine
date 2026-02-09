package net.momirealms.craftengine.proxy.minecraft.world.level.gameevent;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.gameevent.GameEvent")
public interface GameEventProxy {
    GameEventProxy INSTANCE = ASMProxyFactory.create(GameEventProxy.class);
}
