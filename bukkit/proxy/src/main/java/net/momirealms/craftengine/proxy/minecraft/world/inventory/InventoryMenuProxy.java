package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.inventory.InventoryMenu")
public interface InventoryMenuProxy {
    InventoryMenuProxy INSTANCE = ASMProxyFactory.create(InventoryMenuProxy.class);

    @MethodInvoker(name = "slotsChanged")
    void slotsChanged(Object target, @Type(clazz = ContainerProxy.class) Object container);

    @MethodInvoker(name = "getCraftSlots")
    Object getCraftSlots(Object target);
}
