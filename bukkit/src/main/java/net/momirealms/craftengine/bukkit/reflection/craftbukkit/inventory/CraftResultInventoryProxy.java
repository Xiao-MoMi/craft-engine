package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.Inventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftResultInventory")
public interface CraftResultInventoryProxy extends CraftInventoryProxy {
    CraftResultInventoryProxy INSTANCE = ReflectionHelper.getProxy(CraftResultInventoryProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftResultInventoryProxy.class);

    @FieldGetter(name = "resultInventory")
    Object resultInventory(Inventory instance);

    @FieldSetter(name = "resultInventory", strategy = Strategy.MH)
    void resultInventory(Inventory instance, Object resultInventory);
}
