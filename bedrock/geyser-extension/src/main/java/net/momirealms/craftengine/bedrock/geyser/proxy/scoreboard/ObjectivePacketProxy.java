package net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket")
public interface ObjectivePacketProxy {
    ObjectivePacketProxy INSTANCE = ASMProxyFactory.create(ObjectivePacketProxy.class);

    @MethodInvoker(name = "getDisplayName")
    Object displayName(Object packet);
    @ConstructorInvoker
    ClientboundSetObjectivePacket create(String name, ObjectiveAction action, @Type(clazz = AdventureComponentProxy.class) Object title, ScoreType type, NumberFormat format);
}

