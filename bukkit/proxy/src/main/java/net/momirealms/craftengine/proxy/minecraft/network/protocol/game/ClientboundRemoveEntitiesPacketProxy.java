package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
public interface ClientboundRemoveEntitiesPacketProxy {
    ClientboundRemoveEntitiesPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundRemoveEntitiesPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(IntList entityIds);
}
