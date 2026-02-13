package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
public interface ClientboundRemoveEntitiesPacketProxy extends PacketProxy {
    ClientboundRemoveEntitiesPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundRemoveEntitiesPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");

    @ConstructorInvoker
    Object newInstance(IntList entityIds);
}
