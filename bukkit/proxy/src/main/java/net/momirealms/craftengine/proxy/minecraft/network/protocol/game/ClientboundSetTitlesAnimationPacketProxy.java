package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket")
public interface ClientboundSetTitlesAnimationPacketProxy {
    ClientboundSetTitlesAnimationPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetTitlesAnimationPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int fadeIn, int stay, int fadeOut);
}
