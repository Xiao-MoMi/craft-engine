package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.proxy.network.ClientboundCustomPayloadPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureKeyProxy;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;

import java.nio.ByteBuffer;

public final class GeyserCustomPayloads extends PacketTranslator<ClientboundCustomPayloadPacket> {
    private PacketTranslator<ClientboundCustomPayloadPacket> original;

    @SuppressWarnings("unchecked")
    public void install() {
        if (this.original != null) {
            return;
        }
        this.original = (PacketTranslator<ClientboundCustomPayloadPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundCustomPayloadPacket.class);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundCustomPayloadPacket.class, this);
    }

    public void uninstall() {
        if (this.original != null) {
            Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundCustomPayloadPacket.class, this, this.original);
            this.original = null;
        }
    }

    @Override
    public void translate(GeyserSession session, ClientboundCustomPayloadPacket packet) {
        Object channelKey = ClientboundCustomPayloadPacketProxy.INSTANCE.channel(packet);
        String channel = AdventureKeyProxy.INSTANCE.asString(channelKey);
        ClientCustomPacketType<?> type = GeyserCustomPackets.clientbound(channel);
        if (type != null) {
            type.codec().decode(ByteBuffer.wrap(packet.getData())).handle(session);
            return;
        }
        this.original.translate(session, packet);
    }

    public void send(GeyserSession session, ServerCustomPacket packet) {
        ServerCustomPacketType<?> type = GeyserCustomPackets.serverbound(packet.id());
        if (type == null) {
            throw new IllegalArgumentException("Unregistered serverbound custom packet: " + packet.id());
        }
        Object channel = AdventureKeyProxy.INSTANCE.key(type.id());
        session.sendDownstreamGamePacket(ClientboundCustomPayloadPacketProxy.Serverbound.INSTANCE.newInstance(channel, type.encode(packet)));
    }
}
