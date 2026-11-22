package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.ProtocolInfo$Unbound$PacketVisitor", activeIf = "min_version=1.21 && max_version=1.21.4")
public interface UnboundPacketVisitorProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.ProtocolInfo$Unbound$PacketVisitor");
}
