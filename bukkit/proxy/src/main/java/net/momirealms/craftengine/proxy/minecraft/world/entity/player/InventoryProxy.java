package net.momirealms.craftengine.proxy.minecraft.world.entity.player;

import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.function.Predicate;

@ReflectionProxy(name = "net.minecraft.world.entity.player.Inventory")
public interface InventoryProxy {
    InventoryProxy INSTANCE = ASMProxyFactory.create(InventoryProxy.class);

    @MethodInvoker(name = "clearOrCountMatchingItems")
    int clearOrCountMatchingItems(Object target, Predicate<Object> shouldRemove, int maxCount, @Type(clazz = ContainerProxy.class) Object craftingInventory);

    @MethodInvoker(name = "add")
    boolean add(Object target, @Type(clazz = ItemStackProxy.class) Object itemStack);
}
