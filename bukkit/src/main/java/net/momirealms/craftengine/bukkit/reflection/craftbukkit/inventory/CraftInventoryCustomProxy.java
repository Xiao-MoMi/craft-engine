package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryCustom")
public interface CraftInventoryCustomProxy extends CraftInventoryProxy {
    CraftInventoryCustomProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryCustomProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryCustomProxy.class);

    @ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryCustom$MinecraftInventory")
    interface MinecraftInventoryProxy {
        MinecraftInventoryProxy INSTANCE = ReflectionHelper.getProxy(MinecraftInventoryProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(MinecraftInventoryProxy.class);
    }
}
