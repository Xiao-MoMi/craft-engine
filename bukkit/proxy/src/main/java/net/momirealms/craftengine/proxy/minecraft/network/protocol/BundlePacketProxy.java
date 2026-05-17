package net.momirealms.craftengine.proxy.minecraft.network.protocol;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.BundlePacket")
public interface BundlePacketProxy extends PacketProxy {
    BundlePacketProxy INSTANCE = ASMProxyFactory.create(BundlePacketProxy.class);

    @FieldGetter(name = "packets")
    Iterable<Object> getPackets(Object target);

    @FieldSetter(name = "packets")
    void setPackets(Object target, Iterable<?> packets);
}
