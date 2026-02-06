package net.momirealms.craftengine.proxy.minecraft.world.level.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.entity.LevelCallback")
public interface LevelCallbackProxy {
    LevelCallbackProxy INSTANCE = ASMProxyFactory.create(LevelCallbackProxy.class);
}
