package net.momirealms.craftengine.bukkit.reflection.adventure.text;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}Component", ignoreRelocation = true)
public interface ComponentProxy {
    ComponentProxy INSTANCE = ReflectionHelper.getProxy(ComponentProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ComponentProxy.class);
}
