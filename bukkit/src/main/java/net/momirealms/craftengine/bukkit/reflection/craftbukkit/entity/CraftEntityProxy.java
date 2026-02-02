package net.momirealms.craftengine.bukkit.reflection.craftbukkit.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.entity.Entity;

@ReflectionProxy(name = "org.bukkit.craftbukkit.entity.CraftEntity")
public interface CraftEntityProxy {
    CraftEntityProxy INSTANCE = ReflectionHelper.getProxy(CraftEntityProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftEntityProxy.class);

    @FieldGetter(name = "entity")
    Object entity(Entity instance);

    @FieldSetter(name = "entity", strategy = Strategy.MH)
    void entity(Entity instance, Object entity);

    @MethodInvoker(name = "getHandle")
    Object getHandle(Entity instance);
}
