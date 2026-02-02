package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.AnvilInventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryAnvil")
public interface CraftInventoryAnvilProxy extends CraftResultInventoryProxy {
    CraftInventoryAnvilProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryAnvilProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryAnvilProxy.class);

    @FieldGetter(name = "container", version = "1.20~1.20.6")
    Object container(AnvilInventory instance);

    @FieldSetter(name = "container", version = "1.20~1.20.6")
    void container(AnvilInventory instance, Object container);
}
