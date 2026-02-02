package net.momirealms.craftengine.bukkit.reflection.minecraft.world;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.InteractionHand")
public interface InteractionHandProxy {
    InteractionHandProxy INSTANCE = ReflectionHelper.getProxy(InteractionHandProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(InteractionHandProxy.class);
}
