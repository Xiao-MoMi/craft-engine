package net.momirealms.craftengine.bukkit.reflection.minecraft.server.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ServerLevel")
public interface ServerLevelProxy {
    ServerLevelProxy INSTANCE = ReflectionHelper.getProxy(ServerLevelProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ServerLevelProxy.class);
}
