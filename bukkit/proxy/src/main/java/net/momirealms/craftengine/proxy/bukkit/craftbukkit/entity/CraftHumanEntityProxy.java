package net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.entity.CraftHumanEntity")
public interface CraftHumanEntityProxy {
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.entity.CraftHumanEntity");
}
