package net.momirealms.craftengine.bukkit.reflection.adventure.text;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}Component", ignoreRelocation = true)
public interface AdventureComponentProxy {
    AdventureComponentProxy INSTANCE = ReflectionHelper.getProxy(AdventureComponentProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(AdventureComponentProxy.class);
}
