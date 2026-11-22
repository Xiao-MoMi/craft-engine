package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.ProtocolInfo$Details", activeIf = "min_version=1.21.5")
public interface ProtocolDetailsProxy {
    ProtocolDetailsProxy INSTANCE = ASMProxyFactory.create(ProtocolDetailsProxy.class);

    @MethodInvoker(name = "listPackets")
    void listPackets(Object target, @Type(clazz = ProtocolPacketVisitorProxy.class) Object visitor);
}
