package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket")
public interface ClientboundContainerSetContentPacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket");
}