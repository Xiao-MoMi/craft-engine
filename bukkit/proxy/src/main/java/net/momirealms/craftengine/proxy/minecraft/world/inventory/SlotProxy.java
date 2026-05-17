package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.inventory.Slot")
public interface SlotProxy {
    SlotProxy INSTANCE = ASMProxyFactory.create(SlotProxy.class);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target);

    @MethodInvoker(name = "safeTake")
    Object safeTake(Object target, int count, int decrement, @Type(clazz = PlayerProxy.class) Object player);
}
