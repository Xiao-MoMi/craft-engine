package net.momirealms.craftengine.bukkit.reflection.bukkit.event.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.event.block.SignChangeEvent;

@ReflectionProxy(clazz = SignChangeEvent.class)
public interface SignChangeEventProxy {
    SignChangeEventProxy INSTANCE = ReflectionHelper.getProxy(SignChangeEventProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(SignChangeEventProxy.class);
}
