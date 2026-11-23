package net.momirealms.craftengine.proxy.minecraft.world.level.block.piston;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.piston.PistonMovingBlockEntity")
public interface PistonMovingBlockEntityProxy {
    PistonMovingBlockEntityProxy INSTANCE = ASMProxyFactory.create(PistonMovingBlockEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.piston.PistonMovingBlockEntity");

    @MethodInvoker(name = "isExtending")
    boolean isExtending(Object target);
}
