package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom.CustomPacketPayloadProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket"})
public interface ClientboundCustomPayloadPacketProxy {
    ClientboundCustomPayloadPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundCustomPayloadPacketProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.2")
    Object newInstance(@Type(clazz = CustomPacketPayloadProxy.class) Object payload);

    @ConstructorInvoker(activeIf = "max_version=1.20.1")
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object channel, @Type(clazz = FriendlyByteBufProxy.class) Object data);
}
