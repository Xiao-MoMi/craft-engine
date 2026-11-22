package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.RandomSupport")
public interface RandomSupportProxy {
    RandomSupportProxy INSTANCE = ASMProxyFactory.create(RandomSupportProxy.class);

    @MethodInvoker(name = "generateUniqueSeed", isStatic = true)
    long generateUniqueSeed();
}
