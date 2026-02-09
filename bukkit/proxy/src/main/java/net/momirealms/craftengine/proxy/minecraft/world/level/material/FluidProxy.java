package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.Fluid")
public interface FluidProxy {
    FluidProxy INSTANCE = ASMProxyFactory.create(FluidProxy.class);
}
