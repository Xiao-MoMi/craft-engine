package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.InventoryView;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryView")
public interface CraftInventoryViewProxy {
    CraftInventoryViewProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryViewProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryViewProxy.class);

    @FieldGetter(name = "container")
    Object container(InventoryView instance);

    @FieldSetter(name = "container", strategy = Strategy.MH)
    void container(InventoryView instance, Object container);
}
