package net.momirealms.craftengine.bukkit.reflection.minecraft.network;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.PacketSendListener")
public interface PacketSendListenerProxy {
    PacketSendListenerProxy INSTANCE = ReflectionHelper.getProxy(PacketSendListenerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(PacketSendListenerProxy.class);
}
