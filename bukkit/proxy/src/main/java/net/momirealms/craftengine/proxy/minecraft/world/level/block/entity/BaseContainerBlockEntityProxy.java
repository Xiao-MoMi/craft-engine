package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.BaseContainerBlockEntity")
public interface BaseContainerBlockEntityProxy {
    BaseContainerBlockEntityProxy INSTANCE = ASMProxyFactory.create(BaseContainerBlockEntityProxy.class);

    @MethodInvoker(name = "getItem", activeIf = "min_version=1.20.5")
    Object getItem(Object target, int slot);
}
