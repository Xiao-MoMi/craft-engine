package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.ProtocolInfo$Details$PacketVisitor", activeIf = "min_version=1.21.5")
public interface ProtocolPacketVisitorProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.ProtocolInfo$Details$PacketVisitor");
}
