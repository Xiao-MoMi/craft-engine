package net.momirealms.craftengine.proxy.minecraft.world.level.lighting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.lighting.LevelLightEngine")
public interface LevelLightEngineProxy {
    LevelLightEngineProxy INSTANCE = ASMProxyFactory.create(LevelLightEngineProxy.class);


}
