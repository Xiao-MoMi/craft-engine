package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.LevelReader")
public interface LevelReaderProxy {
    LevelReaderProxy INSTANCE = ASMProxyFactory.create(LevelReaderProxy.class);

    @MethodInvoker(name = "dimensionType")
    Object dimensionType(Object target);

    @MethodInvoker(name = "getNoiseBiome")
    Object getNoiseBiome(Object target, int x, int y, int z);

    @MethodInvoker(name = "getMaxLocalRawBrightness")
    int getMaxLocalRawBrightness(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);
}
