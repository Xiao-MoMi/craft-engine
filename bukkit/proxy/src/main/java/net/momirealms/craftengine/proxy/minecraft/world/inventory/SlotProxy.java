package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.inventory.Slot")
public interface SlotProxy {
    SlotProxy INSTANCE = ASMProxyFactory.create(SlotProxy.class);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target);
}
