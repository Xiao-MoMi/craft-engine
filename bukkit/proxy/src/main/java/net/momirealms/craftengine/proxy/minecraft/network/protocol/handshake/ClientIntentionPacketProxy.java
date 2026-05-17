package net.momirealms.craftengine.proxy.minecraft.network.protocol.handshake;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.handshake.ClientIntentionPacket")
public interface ClientIntentionPacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.handshake.ClientIntentionPacket");
}
