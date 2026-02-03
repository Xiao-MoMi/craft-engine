package net.momirealms.craftengine.bukkit.reflection.minecraft.world.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.World;

@ReflectionProxy(name = "net.minecraft.world.level.Level")
public interface LevelProxy {
    LevelProxy INSTANCE = ReflectionHelper.getProxy(LevelProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(LevelProxy.class);

    @MethodInvoker(name = "getWorld")
    World getWorld(Object instance);

    @MethodInvoker(name = "moonrise$getEntityLookup", version = ">=1.21")
    Object moonrise$getEntityLookup(Object instance);
}
