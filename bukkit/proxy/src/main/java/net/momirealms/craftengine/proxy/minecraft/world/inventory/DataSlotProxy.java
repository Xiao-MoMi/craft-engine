package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.inventory.DataSlot")
public interface DataSlotProxy {
    DataSlotProxy INSTANCE = ASMProxyFactory.create(DataSlotProxy.class);

    @MethodInvoker(name = "get")
    int get(Object target);
}
