package net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity.player;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.player.Player")
public interface PlayerProxy {
    PlayerProxy INSTANCE = ReflectionHelper.getProxy(PlayerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(PlayerProxy.class);
}
