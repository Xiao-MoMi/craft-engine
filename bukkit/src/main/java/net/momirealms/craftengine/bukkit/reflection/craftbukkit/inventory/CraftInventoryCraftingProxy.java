package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.CraftingInventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryCrafting")
public interface CraftInventoryCraftingProxy extends CraftInventoryProxy {
    CraftInventoryCraftingProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryCraftingProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryCraftingProxy.class);

    @FieldGetter(name = "resultInventory")
    Object resultInventory(CraftingInventory instance);

    @FieldSetter(name = "resultInventory", strategy = Strategy.MH)
    void resultInventory(CraftingInventory instance, Object resultInventory);
}
