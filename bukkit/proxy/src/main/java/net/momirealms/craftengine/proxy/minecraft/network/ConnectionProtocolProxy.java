package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketFlowProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.network.ConnectionProtocol", activeIf = "max_version=1.20.4")
public interface ConnectionProtocolProxy {
    ConnectionProtocolProxy INSTANCE = ASMProxyFactory.create(ConnectionProtocolProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.ConnectionProtocol");

    @MethodInvoker(name = "getPacketsByIds")
    Map<Integer, Class<?>> getPacketsByIds(Object target, @Type(clazz = PacketFlowProxy.class) Object flow);
}
