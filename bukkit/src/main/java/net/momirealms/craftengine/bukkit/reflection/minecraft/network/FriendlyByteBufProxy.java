package net.momirealms.craftengine.bukkit.reflection.minecraft.network;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.FriendlyByteBuf")
public interface FriendlyByteBufProxy {
    FriendlyByteBufProxy INSTANCE = ReflectionHelper.getProxy(FriendlyByteBufProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(FriendlyByteBufProxy.class);
}
