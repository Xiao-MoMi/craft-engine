package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.network.ClientboundCustomPayloadPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureKeyProxy;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;

import java.nio.ByteBuffer;

public final class GeyserCustomPayloads implements GeyserJavaPackets.Interceptor<ClientboundCustomPayloadPacket> {
    @Override
    public void translate(GeyserPlayer player, ClientboundCustomPayloadPacket packet, PacketTranslator<ClientboundCustomPayloadPacket> original) {
        Object channelKey = ClientboundCustomPayloadPacketProxy.INSTANCE.channel(packet);
        String channel = AdventureKeyProxy.INSTANCE.asString(channelKey);
        ClientCustomPacketType<?> type = GeyserCustomPackets.clientbound(channel);
        if (type != null) {
            type.codec().decode(ByteBuffer.wrap(packet.getData())).handle(player);
            return;
        }
        original.translate(player.session(), packet);
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
