package net.momirealms.craftengine.bukkit.reflection.minecraft.server.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ServerLevel")
public interface ServerLevelProxy extends LevelProxy {
    ServerLevelProxy INSTANCE = ReflectionHelper.getProxy(ServerLevelProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ServerLevelProxy.class);

    @MethodInvoker(name = "getEntityLookup", version = "<1.21")
    Object getEntityLookup(Object instance);
}
