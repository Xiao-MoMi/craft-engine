package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.scores.PlayerTeamProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket")
public interface ClientboundSetPlayerTeamPacketProxy {
    ClientboundSetPlayerTeamPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetPlayerTeamPacketProxy.class);

    @MethodInvoker(name = "createAddOrModifyPacket", isStatic = true)
    Object createAddOrModifyPacket(@Type(clazz = PlayerTeamProxy.class) Object team, boolean useAdd);
}
