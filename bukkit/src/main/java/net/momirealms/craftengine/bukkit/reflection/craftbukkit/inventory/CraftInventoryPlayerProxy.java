package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryPlayer")
public interface CraftInventoryPlayerProxy extends CraftInventoryProxy {
    CraftInventoryPlayerProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryPlayerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryPlayerProxy.class);

    @MethodInvoker(name = "getInventory")
    Object getInventory(Object instance);
}
