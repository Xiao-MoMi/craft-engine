package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket")
public interface SystemChatPacketProxy {
    SystemChatPacketProxy INSTANCE = ASMProxyFactory.create(SystemChatPacketProxy.class);

    @MethodInvoker(name = "getContent")
    Object text(Object packet);
}
