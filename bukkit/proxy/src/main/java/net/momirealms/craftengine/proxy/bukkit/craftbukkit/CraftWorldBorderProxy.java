package net.momirealms.craftengine.proxy.bukkit.craftbukkit;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftWorldBorder")
public interface CraftWorldBorderProxy {
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.CraftWorldBorder");
}
