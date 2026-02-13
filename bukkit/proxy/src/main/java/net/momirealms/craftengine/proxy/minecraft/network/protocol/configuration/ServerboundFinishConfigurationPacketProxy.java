package net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket", activeIf = "min_version=1.20.2")
public interface ServerboundFinishConfigurationPacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket");
}