package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket")
public interface SetTitleTextPacketProxy {
    SetTitleTextPacketProxy INSTANCE = ASMProxyFactory.create(SetTitleTextPacketProxy.class);

    @MethodInvoker(name = "getText")
    Object text(Object packet);

    @MethodInvoker(name = "withText")
    ClientboundSetTitleTextPacket withText(Object packet, @Type(clazz = AdventureComponentProxy.class) Object text);
}
