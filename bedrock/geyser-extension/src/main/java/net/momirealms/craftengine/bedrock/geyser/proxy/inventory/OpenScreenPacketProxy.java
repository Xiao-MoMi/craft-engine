package net.momirealms.craftengine.bedrock.geyser.proxy.inventory;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket")
public interface OpenScreenPacketProxy {
    OpenScreenPacketProxy INSTANCE = ASMProxyFactory.create(OpenScreenPacketProxy.class);

    @MethodInvoker(name = "getTitle")
    Object title(Object packet);

    @MethodInvoker(name = "withTitle")
    ClientboundOpenScreenPacket withTitle(Object packet, @Type(clazz = AdventureComponentProxy.class) Object title);
}
