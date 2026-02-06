package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.FenceGateBlock")
public interface FenceGateBlockProxy extends BlockProxy {
    FenceGateBlockProxy INSTANCE = ASMProxyFactory.create(FenceGateBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.FenceGateBlock");
}
