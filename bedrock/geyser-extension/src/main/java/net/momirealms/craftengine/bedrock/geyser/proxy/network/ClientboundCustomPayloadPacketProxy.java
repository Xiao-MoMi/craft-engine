package net.momirealms.craftengine.bedrock.geyser.proxy.network;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket")
public interface ClientboundCustomPayloadPacketProxy {
    ClientboundCustomPayloadPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundCustomPayloadPacketProxy.class);

    @MethodInvoker(name = "getChannel")
    Object channel(Object packet);

    @ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket")
    interface Serverbound {
        Serverbound INSTANCE = ASMProxyFactory.create(Serverbound.class);

        @ConstructorInvoker
        ServerboundCustomPayloadPacket newInstance(@Type(clazz = AdventureKeyProxy.class) Object channel, byte[] data);
    }
}
