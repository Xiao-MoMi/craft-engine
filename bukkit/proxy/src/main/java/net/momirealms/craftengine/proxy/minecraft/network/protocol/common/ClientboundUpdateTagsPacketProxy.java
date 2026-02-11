package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket", "net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket"})
public interface ClientboundUpdateTagsPacketProxy extends PacketProxy {
    ClientboundUpdateTagsPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundUpdateTagsPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(Map<?, ?> tags);

    @FieldGetter(name = "tags")
    Map<Object, Object> getTags(Object target);
}
