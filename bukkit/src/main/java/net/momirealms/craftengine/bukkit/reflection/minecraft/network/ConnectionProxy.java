package net.momirealms.craftengine.bukkit.reflection.minecraft.network;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.Connection")
public interface ConnectionProxy {
    ConnectionProxy INSTANCE = ReflectionHelper.getProxy(ConnectionProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ConnectionProxy.class);
}
