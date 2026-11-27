package net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket")
public interface ScorePacketProxy {
    ScorePacketProxy INSTANCE = ASMProxyFactory.create(ScorePacketProxy.class);

    @MethodInvoker(name = "getDisplay")
    Object display(Object packet);
    @MethodInvoker(name = "withDisplay")
    ClientboundSetScorePacket withDisplay(Object packet, @Type(clazz = AdventureComponentProxy.class) Object display);
}

