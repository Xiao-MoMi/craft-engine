package net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EntityType")
public interface EntityTypeProxy {
    EntityTypeProxy INSTANCE = ReflectionHelper.getProxy(EntityTypeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(EntityTypeProxy.class);
}
