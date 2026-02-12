package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity")
public interface AbstractFurnaceBlockEntityProxy extends BaseContainerBlockEntityProxy {
    AbstractFurnaceBlockEntityProxy INSTANCE = ASMProxyFactory.create(AbstractFurnaceBlockEntityProxy.class);

    @MethodInvoker(name = "getItem", activeIf = "max_version=1.20.4")
    Object getItem(Object target, int slot);
}
