package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.RedStoneWireBlock")
public interface RedstoneWireBlockProxy extends BlockProxy {
    RedstoneWireBlockProxy INSTANCE = ASMProxyFactory.create(RedstoneWireBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.RedstoneWireBlock");
}
