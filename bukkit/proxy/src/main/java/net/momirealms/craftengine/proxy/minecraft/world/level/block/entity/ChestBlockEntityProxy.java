package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.ChestBlockEntity")
public interface ChestBlockEntityProxy {
    ChestBlockEntityProxy INSTANCE = ASMProxyFactory.create(ChestBlockEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.entity.ChestBlockEntity");
}
