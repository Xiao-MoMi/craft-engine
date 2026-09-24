package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.RandomState")
public interface RandomStateProxy {
    RandomStateProxy INSTANCE = ASMProxyFactory.create(RandomStateProxy.class);
    @MethodInvoker(name = "createClimateSampler", activeIf = "min_version=26.3")
    Object createClimateSampler(Object target, @Type(clazz = net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.densityfunction.SamplerContextProxy.class) Object context);
}
