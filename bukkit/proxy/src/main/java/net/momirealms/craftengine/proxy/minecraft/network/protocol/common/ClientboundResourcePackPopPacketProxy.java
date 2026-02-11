package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Optional;
import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket", activeIf = "min_version=1.20.3")
public interface ClientboundResourcePackPopPacketProxy {
    ClientboundResourcePackPopPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundResourcePackPopPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(Optional<UUID> id);
}
