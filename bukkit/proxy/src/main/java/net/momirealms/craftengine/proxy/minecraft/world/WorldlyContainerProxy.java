package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.WorldlyContainer")
public interface WorldlyContainerProxy extends ContainerProxy {
    WorldlyContainerProxy INSTANCE = ASMProxyFactory.create(WorldlyContainerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.WorldlyContainer");
}
