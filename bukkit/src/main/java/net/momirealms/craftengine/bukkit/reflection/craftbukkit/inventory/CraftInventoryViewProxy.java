package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryView")
public interface CraftInventoryViewProxy {
    CraftInventoryViewProxy INSTANCE = ReflectionHelper.getProxy(CraftInventoryViewProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftInventoryViewProxy.class);

    @FieldGetter(name = "container")
    Object container(Object instance);

    @FieldSetter(name = "container", strategy = Strategy.MH)
    void container(Object instance, Object container);
}
