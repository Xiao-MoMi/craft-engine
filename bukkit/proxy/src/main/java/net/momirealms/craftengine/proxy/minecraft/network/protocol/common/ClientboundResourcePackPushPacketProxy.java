package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket", "net.minecraft.network.protocol.common.ClientboundResourcePackPacket", "net.minecraft.network.protocol.game.ClientboundResourcePackPacket"})
public interface ClientboundResourcePackPushPacketProxy extends PacketProxy {
    ClientboundResourcePackPushPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundResourcePackPushPacketProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance(UUID id, String url, String hash, boolean required, Optional<?> prompt);

    @ConstructorInvoker(activeIf = "version=1.20.3 || version=1.20.4")
    Object newInstance(UUID id, String url, String hash, boolean required, @Nullable @Type(clazz = ComponentProxy.class) Object prompt);

    @ConstructorInvoker(activeIf = "max_version=1.20.2")
    Object newInstance(String url, String hash, boolean required, @Nullable @Type(clazz = ComponentProxy.class) Object prompt);
}
