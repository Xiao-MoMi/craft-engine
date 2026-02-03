package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBundlePacket")
public interface ClientboundBundlePacketProxy {
    ClientboundBundlePacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundBundlePacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundBundlePacketProxy.class);
}
