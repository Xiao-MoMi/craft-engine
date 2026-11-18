package net.momirealms.craftengine.bedrock.geyser.proxy.network;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket")
public interface BossEventPacketProxy {
    BossEventPacketProxy INSTANCE = ASMProxyFactory.create(BossEventPacketProxy.class);

    @MethodInvoker(name = "getTitle")
    Object title(Object packet);

    @MethodInvoker(name = "withTitle")
    ClientboundBossEventPacket withTitle(Object packet, @Type(clazz = AdventureComponentProxy.class) Object title);
}
