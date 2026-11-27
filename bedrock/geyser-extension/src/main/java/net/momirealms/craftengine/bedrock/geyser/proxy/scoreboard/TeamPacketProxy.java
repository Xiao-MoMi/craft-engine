package net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket")
public interface TeamPacketProxy {
    TeamPacketProxy INSTANCE = ASMProxyFactory.create(TeamPacketProxy.class);

    @MethodInvoker(name = "getPlayerPrefix")
    Object prefix(Object packet);

    @MethodInvoker(name = "getPlayerSuffix")
    Object suffix(Object packet);
    @ConstructorInvoker
    ClientboundSetPlayerTeamPacket create(String name, @Type(clazz = AdventureComponentProxy.class) Object display,
            @Type(clazz = AdventureComponentProxy.class) Object prefix, @Type(clazz = AdventureComponentProxy.class) Object suffix,
            boolean friendlyFire, boolean seeInvisible, NameTagVisibility visibility, CollisionRule collision, TeamColor color, String[] players);
}

