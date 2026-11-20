package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.jetbrains.annotations.NotNull;

public final class GeyserJavaPacketHandler extends SessionAdapter {
    private final GeyserPlayer player;
    private final GeyserJavaPackets packets;
    private volatile Session connection;

    public GeyserJavaPacketHandler(GeyserPlayer player, GeyserJavaPackets packets) {
        this.player = player;
        this.packets = packets;
    }

    public synchronized boolean attach(Session connection) {
        if (this.connection == connection || this.player.isClosed()) {
            return false;
        }
        this.close();
        this.connection = connection;
        connection.addListener(this);
        return true;
    }

    public synchronized void close() {
        Session previous = this.connection;
        if (previous != null) {
            this.connection = null;
            previous.removeListener(this);
        }
    }

    @Override
    public void packetReceived(@NotNull Session session, @NotNull Packet packet) {
        if (session == this.connection && !this.player.isClosed()) {
            this.packets.receive(this.player, packet);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        if (event.getSession() == this.connection && !this.player.isClosed()) {
            this.packets.sending(this.player, event);
        }
    }

    @Override
    public void packetSent(@NotNull Session session, @NotNull Packet packet) {
        if (session == this.connection && !this.player.isClosed()) {
            this.packets.sent(this.player, packet);
        }
    }
}
