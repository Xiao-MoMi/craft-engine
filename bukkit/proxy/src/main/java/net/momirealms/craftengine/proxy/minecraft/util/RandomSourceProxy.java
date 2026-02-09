package net.momirealms.craftengine.proxy.minecraft.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.RandomSource")
public interface RandomSourceProxy {
    RandomSourceProxy INSTANCE = ASMProxyFactory.create(RandomSourceProxy.class);
}
