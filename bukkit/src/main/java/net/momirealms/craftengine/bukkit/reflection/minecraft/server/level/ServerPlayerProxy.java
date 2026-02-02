package net.momirealms.craftengine.bukkit.reflection.minecraft.server.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ServerPlayer")
public interface ServerPlayerProxy {
    ServerPlayerProxy INSTANCE = ReflectionHelper.getProxy(ServerPlayerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ServerPlayerProxy.class);
}
