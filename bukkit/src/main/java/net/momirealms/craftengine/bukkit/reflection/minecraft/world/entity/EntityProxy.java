package net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Entity")
public interface EntityProxy {
    EntityProxy INSTANCE = ReflectionHelper.getProxy(EntityProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(EntityProxy.class);
}
