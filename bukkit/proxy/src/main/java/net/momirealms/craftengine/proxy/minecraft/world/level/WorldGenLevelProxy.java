package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.WorldGenLevel")
public interface WorldGenLevelProxy {
    WorldGenLevelProxy INSTANCE = ASMProxyFactory.create(WorldGenLevelProxy.class);
}
