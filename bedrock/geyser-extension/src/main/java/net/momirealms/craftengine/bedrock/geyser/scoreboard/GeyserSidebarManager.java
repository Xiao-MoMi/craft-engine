package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.proxy.session.GeyserSessionProxy;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.UpstreamSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserSidebarManager {
    private final ContainerTitleLayout layout;
    private final SidebarProfiles profiles;
    private final Map<GeyserSession, Connection> sessions = new ConcurrentHashMap<>();
    private final List<Capture<?>> translators = new ArrayList<>();

    public GeyserSidebarManager(ContainerTitleLayout layout, SidebarProfiles profiles) {
        this.layout = layout;
        this.profiles = profiles;
    }

    public void install() {
        this.register(ClientboundSetObjectivePacket.class);
        this.register(ClientboundSetScorePacket.class);
        this.register(ClientboundResetScorePacket.class);
        this.register(ClientboundSetPlayerTeamPacket.class);
        this.register(ClientboundSetDisplayObjectivePacket.class);
    }

    @SuppressWarnings("unchecked")
    private <P extends Packet> void register(Class<P> type) {
        PacketTranslator<P> original = (PacketTranslator<P>) Registries.JAVA_PACKET_TRANSLATORS.get(type);
        Capture<P> capture = new Capture<>(type, original);
        this.translators.add(capture);
        Registries.JAVA_PACKET_TRANSLATORS.register(type, capture);
    }

    public SidebarSession session(GeyserSession session) {
        return this.sessions.computeIfAbsent(session, current -> {
            SidebarSession sidebar = new SidebarSession(current, this.layout, this.profiles);
            UpstreamSession original = current.getUpstream();
            UpstreamSession wrapped = new SidebarUpstream(original, sidebar).wrap();
            GeyserSessionProxy.INSTANCE.upstream(current, wrapped);
            return new Connection(sidebar, original, wrapped);
        }).sidebar();
    }

    public void disconnect(Object session) {
        Connection connection = this.sessions.remove(session);
        if (connection != null && session instanceof GeyserSession geyser && geyser.getUpstream() == connection.wrapped()) {
            GeyserSessionProxy.INSTANCE.upstream(geyser, connection.original());
        }
    }

    public void uninstall() {
        for (Capture<?> capture : this.translators) {
            Registries.JAVA_PACKET_TRANSLATORS.get().replace(capture.type, capture, capture.original);
        }
        this.translators.clear();
        for (GeyserSession session : List.copyOf(this.sessions.keySet())) {
            this.disconnect(session);
        }
    }

    private record Connection(SidebarSession sidebar, UpstreamSession original, UpstreamSession wrapped) {
    }

    private final class Capture<P extends Packet> extends PacketTranslator<P> {
        private final Class<P> type;
        private final PacketTranslator<P> original;

        private Capture(Class<P> type, PacketTranslator<P> original) {
            this.type = type;
            this.original = original;
        }

        @Override
        public void translate(GeyserSession session, P packet) {
            SidebarSession sidebar = GeyserSidebarManager.this.session(session);
            // Geyser also flushes scoreboards on its updater thread; keep source components
            // and the state mutation they describe together when output is assembled.
            synchronized (sidebar) {
                sidebar.capture(packet);
                this.original.translate(session, packet);
            }
        }
    }
}
