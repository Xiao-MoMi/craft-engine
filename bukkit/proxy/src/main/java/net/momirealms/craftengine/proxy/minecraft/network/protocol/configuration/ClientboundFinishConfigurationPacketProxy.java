package net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket", activeIf = "min_version=1.20.2")
public interface ClientboundFinishConfigurationPacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket");
}