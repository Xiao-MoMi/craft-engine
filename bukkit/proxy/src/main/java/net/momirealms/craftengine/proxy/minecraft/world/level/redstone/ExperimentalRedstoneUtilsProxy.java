package net.momirealms.craftengine.proxy.minecraft.world.level.redstone;

import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;

@ReflectionProxy(name = "net.minecraft.world.level.redstone.ExperimentalRedstoneUtils", activeIf = "min_version=1.21.2")
public interface ExperimentalRedstoneUtilsProxy {
    ExperimentalRedstoneUtilsProxy INSTANCE = ASMProxyFactory.create(ExperimentalRedstoneUtilsProxy.class);

    @MethodInvoker(name = "initialOrientation", isStatic = true)
    Object initialOrientation(@Type(clazz = LevelProxy.class) Object world, @Nullable @Type(clazz = DirectionProxy.class) Object up, @Nullable @Type(clazz = DirectionProxy.class) Object front);
}
