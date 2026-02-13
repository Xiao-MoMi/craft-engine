package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.LevelHeightAccessor")
public interface LevelHeightAccessorProxy {
    LevelHeightAccessorProxy INSTANCE = ASMProxyFactory.create(LevelHeightAccessorProxy.class);
}
