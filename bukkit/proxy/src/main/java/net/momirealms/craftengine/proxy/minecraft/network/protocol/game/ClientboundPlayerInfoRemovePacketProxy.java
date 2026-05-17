package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;
import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
public interface ClientboundPlayerInfoRemovePacketProxy extends PacketProxy {
    ClientboundPlayerInfoRemovePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundPlayerInfoRemovePacketProxy.class);

    @ConstructorInvoker
    Object newInstance(List<UUID> profileIds);
}
