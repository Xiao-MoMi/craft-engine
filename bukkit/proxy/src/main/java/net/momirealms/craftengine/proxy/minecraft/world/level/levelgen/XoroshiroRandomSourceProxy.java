package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.XoroshiroRandomSource")
public interface XoroshiroRandomSourceProxy {
    XoroshiroRandomSourceProxy INSTANCE = ASMProxyFactory.create(XoroshiroRandomSourceProxy.class);

    @ConstructorInvoker
    Object newInstance(long seed);
}
