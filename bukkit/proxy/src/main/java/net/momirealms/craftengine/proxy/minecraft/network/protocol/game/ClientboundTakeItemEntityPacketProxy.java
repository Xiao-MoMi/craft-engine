package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket")
public interface ClientboundTakeItemEntityPacketProxy {
    ClientboundTakeItemEntityPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundTakeItemEntityPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int itemId, int playerId, int amount);
}
