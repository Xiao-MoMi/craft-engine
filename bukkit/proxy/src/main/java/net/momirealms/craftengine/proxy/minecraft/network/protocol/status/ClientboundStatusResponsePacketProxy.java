package net.momirealms.craftengine.proxy.minecraft.network.protocol.status;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.status.ClientboundStatusResponsePacket")
public interface ClientboundStatusResponsePacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.status.ClientboundStatusResponsePacket");
}