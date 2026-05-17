package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket")
public interface ClientboundSetActionBarTextPacketProxy extends PacketProxy {
    ClientboundSetActionBarTextPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetActionBarTextPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ComponentProxy.class) Object text);
}
