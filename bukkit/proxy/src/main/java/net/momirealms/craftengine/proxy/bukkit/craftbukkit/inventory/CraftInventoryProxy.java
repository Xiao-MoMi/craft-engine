package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.inventory.Inventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventory")
public interface CraftInventoryProxy {
    CraftInventoryProxy INSTANCE = ASMProxyFactory.create(CraftInventoryProxy.class);
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.inventory.CraftInventory");

    @ConstructorInvoker
    Inventory newInstance(@Type(clazz = ContainerProxy.class) Object container);

    @FieldGetter(name = "inventory")
    Object getInventory(Object target);

    @FieldSetter(name = "inventory")
    void setInventory(Object target, Object inventory);
}
