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
    private Session connection;
    private boolean attached;

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
        return this.activate();
    }

    public synchronized boolean activate() {
        if (this.attached || this.connection == null || this.player.isClosed() || !this.packets.hasObservers()) {
            return false;
        }
        this.connection.addListener(this);
        this.attached = true;
        return true;
    }

    public synchronized void close() {
        Session previous = this.connection;
        this.connection = null;
        if (this.attached) {
            this.attached = false;
            previous.removeListener(this);
        }
    }

    @Override
    public void packetReceived(@NotNull Session session, @NotNull Packet packet) {
        this.packets.receive(this.player, packet);
    }

    @Override
    public void packetSending(@NotNull PacketSendingEvent event) {
        this.packets.sending(this.player, event);
    }

    @Override
    public void packetSent(@NotNull Session session, @NotNull Packet packet) {
        this.packets.sent(this.player, packet);
    }
}
