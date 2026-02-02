package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.Inventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventory")
public interface CraftInventoryProxy {
    CraftInventoryProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryProxy.class);

    @MethodInvoker(name = "getInventory")
    Object getInventory(Inventory instance);
}
