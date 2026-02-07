package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.FluidState")
public interface FluidStateProxy {
    FluidStateProxy INSTANCE = ASMProxyFactory.create(FluidStateProxy.class);

    @MethodInvoker(name = "createLegacyBlock")
    Object createLegacyBlock(Object target);
}
