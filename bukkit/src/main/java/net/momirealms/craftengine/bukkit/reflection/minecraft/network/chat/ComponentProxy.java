package net.momirealms.craftengine.bukkit.reflection.minecraft.network.chat;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.chat.Component")
public interface ComponentProxy {
    ComponentProxy INSTANCE = ReflectionHelper.getProxy(ComponentProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ComponentProxy.class);
}
