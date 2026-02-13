package net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.custom.CustomPacketPayload")
public interface CustomPacketPayloadProxy {
    CustomPacketPayloadProxy INSTANCE = ASMProxyFactory.create(CustomPacketPayloadProxy.class);


}
