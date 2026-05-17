package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.WorldlyContainer")
public interface WorldlyContainerProxy extends ContainerProxy {
    WorldlyContainerProxy INSTANCE = ASMProxyFactory.create(WorldlyContainerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.WorldlyContainer");

    @MethodInvoker(name = "getSlotsForFace")
    int[] getSlotsForFace(Object target, @Type(clazz = DirectionProxy.class) Object side);
}
