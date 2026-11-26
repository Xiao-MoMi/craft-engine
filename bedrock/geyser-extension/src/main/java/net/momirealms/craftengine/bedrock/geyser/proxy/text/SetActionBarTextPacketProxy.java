package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket")
public interface SetActionBarTextPacketProxy {
    SetActionBarTextPacketProxy INSTANCE = ASMProxyFactory.create(SetActionBarTextPacketProxy.class);

    @MethodInvoker(name = "getText")
    Object text(Object packet);
}
