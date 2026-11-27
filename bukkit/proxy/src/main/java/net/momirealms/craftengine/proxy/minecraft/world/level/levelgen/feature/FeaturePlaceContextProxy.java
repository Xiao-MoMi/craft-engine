package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.FeaturePlaceContext", activeIf = "max_version=26.2")
public interface FeaturePlaceContextProxy {
    FeaturePlaceContextProxy INSTANCE = ASMProxyFactory.create(FeaturePlaceContextProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.levelgen.feature.FeaturePlaceContext");

    @MethodInvoker(name = "level")
    Object level(Object target);

    @MethodInvoker(name = "random")
    Object random(Object target);

    @MethodInvoker(name = "origin")
    Object origin(Object target);

    @MethodInvoker(name = "config")
    Object config(Object target);
}
