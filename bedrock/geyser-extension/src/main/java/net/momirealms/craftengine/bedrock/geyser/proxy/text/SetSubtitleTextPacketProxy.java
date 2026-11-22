package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket")
public interface SetSubtitleTextPacketProxy {
    SetSubtitleTextPacketProxy INSTANCE = ASMProxyFactory.create(SetSubtitleTextPacketProxy.class);

    @MethodInvoker(name = "getText")
    Object text(Object packet);

    @MethodInvoker(name = "withText")
    ClientboundSetSubtitleTextPacket withText(Object packet, @Type(clazz = AdventureComponentProxy.class) Object text);
}
