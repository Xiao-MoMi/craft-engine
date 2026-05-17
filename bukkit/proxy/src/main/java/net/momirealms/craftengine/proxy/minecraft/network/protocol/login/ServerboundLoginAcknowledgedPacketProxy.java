package net.momirealms.craftengine.proxy.minecraft.network.protocol.login;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket", activeIf = "min_version=1.20.2")
public interface ServerboundLoginAcknowledgedPacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket");
}
