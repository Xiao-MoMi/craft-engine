package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundAddPlayerPacket", version = "<=1.20.1")
public interface ClientboundAddPlayerPacketProxy {
    ClientboundAddPlayerPacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundAddPlayerPacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundAddPlayerPacketProxy.class);
}
