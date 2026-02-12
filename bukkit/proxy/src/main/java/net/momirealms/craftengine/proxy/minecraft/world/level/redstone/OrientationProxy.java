package net.momirealms.craftengine.proxy.minecraft.world.level.redstone;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.redstone.Orientation", activeIf = "min_version=1.21.2")
public interface OrientationProxy {
    OrientationProxy INSTANCE = ASMProxyFactory.create(OrientationProxy.class);
}
