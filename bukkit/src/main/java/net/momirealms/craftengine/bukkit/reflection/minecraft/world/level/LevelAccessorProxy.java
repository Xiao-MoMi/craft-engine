package net.momirealms.craftengine.bukkit.reflection.minecraft.world.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.LevelAccessor")
public interface LevelAccessorProxy {
    LevelAccessorProxy INSTANCE = ReflectionHelper.getProxy(LevelAccessorProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(LevelAccessorProxy.class);
}
