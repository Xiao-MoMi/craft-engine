package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.ClientboundShowDialogPacket")
public interface ClientboundShowDialogPacketProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ClientboundShowDialogPacket");
}
