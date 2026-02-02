package net.momirealms.craftengine.bukkit.reflection.craftbukkit;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftWorld")
public interface CraftWorldProxy {
    CraftWorldProxy INSTANCE = ReflectionHelper.getProxy(CraftWorldProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftWorldProxy.class);
}
