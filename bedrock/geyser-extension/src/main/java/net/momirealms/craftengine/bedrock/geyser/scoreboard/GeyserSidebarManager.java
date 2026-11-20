package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;

import java.util.function.BiConsumer;

public final class GeyserSidebarManager {
    private final ContainerTitleLayout layout;
    private final SidebarProfiles profiles;

    public GeyserSidebarManager(ContainerTitleLayout layout, SidebarProfiles profiles) {
        this.layout = layout;
        this.profiles = profiles;
    }

    public void install(GeyserJavaPackets javaPackets) {
        this.intercept(javaPackets, ClientboundSetObjectivePacket.class, SidebarSession::capture);
        this.intercept(javaPackets, ClientboundSetScorePacket.class, SidebarSession::capture);
        this.intercept(javaPackets, ClientboundResetScorePacket.class, SidebarSession::capture);
        this.intercept(javaPackets, ClientboundSetPlayerTeamPacket.class, SidebarSession::capture);
        this.intercept(javaPackets, ClientboundSetDisplayObjectivePacket.class, SidebarSession::capture);
    }

    private <P extends Packet> void intercept(GeyserJavaPackets javaPackets, Class<P> type, BiConsumer<SidebarSession, P> capture) {
        javaPackets.intercept(type, (player, packet, original) -> {
            GeyserSession session = player.session();
            SidebarSession sidebar = player.sidebar();
            if (sidebar == null) {
                original.translate(session, packet);
                return;
            }
            // Geyser also flushes scoreboards on its updater thread; keep source components
            // and the state mutation they describe together when output is assembled.
            synchronized (sidebar) {
                capture.accept(sidebar, packet);
                original.translate(session, packet);
            }
        });
    }

    public GeyserSidebarConnection createConnection(GeyserSession session) {
        return new GeyserSidebarConnection(session, this.layout, this.profiles);
    }
}
