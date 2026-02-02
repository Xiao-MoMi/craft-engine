package net.momirealms.craftengine.bukkit.reflection.craftbukkit.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.entity.CraftPlayer")
public interface CraftPlayerProxy {
    CraftPlayerProxy INSTANCE = ReflectionHelper.getProxy(CraftPlayerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftPlayerProxy.class);
}
