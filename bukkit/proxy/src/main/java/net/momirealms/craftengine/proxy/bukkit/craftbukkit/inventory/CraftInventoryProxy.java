package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventory")
public interface CraftInventoryProxy {
    CraftInventoryProxy INSTANCE = ASMProxyFactory.create(CraftInventoryProxy.class);
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.inventory.CraftInventory");

    @FieldGetter(name = "inventory")
    Object getInventory(Object target);

    @FieldSetter(name = "inventory")
    void setInventory(Object target, Object inventory);
}
