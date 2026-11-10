package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.network.protocol.ClientboundBedrockHandshakePacket;
import net.momirealms.craftengine.bedrock.geyser.network.protocol.ClientboundHardHitboxPacket;
import net.momirealms.craftengine.bedrock.geyser.network.protocol.ServerboundBedrockHandshakePacket;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

import java.util.HashMap;
import java.util.Map;

public final class GeyserCustomPackets {
    private static final Map<String, ClientCustomPacketType<?>> CLIENTBOUND = new HashMap<>();
    private static final Map<String, ServerCustomPacketType<?>> SERVERBOUND = new HashMap<>();
    public static final ClientCustomPacketType<ClientboundHardHitboxPacket> HARD_HITBOX = registerClientbound(ClientboundHardHitboxPacket.ID, ClientboundHardHitboxPacket.CODEC);
    public static final ClientCustomPacketType<ClientboundBedrockHandshakePacket> BEDROCK_HANDSHAKE_REQUEST = registerClientbound(ClientboundBedrockHandshakePacket.ID, ClientboundBedrockHandshakePacket.CODEC);
    public static final ServerCustomPacketType<ServerboundBedrockHandshakePacket> BEDROCK_HANDSHAKE = registerServerbound(ServerboundBedrockHandshakePacket.ID, ServerboundBedrockHandshakePacket.class, ServerboundBedrockHandshakePacket.CODEC);

    private GeyserCustomPackets() {
    }

    public static <T extends ClientCustomPacket> ClientCustomPacketType<T> registerClientbound(String id, PacketCodec<T> codec) {
        ClientCustomPacketType<T> type = new ClientCustomPacketType<>(id, codec);
        if (CLIENTBOUND.putIfAbsent(id, type) != null) {
            throw new IllegalArgumentException("Duplicate clientbound custom packet: " + id);
        }
        return type;
    }

    public static <T extends ServerCustomPacket> ServerCustomPacketType<T> registerServerbound(String id, Class<T> packetClass, PacketCodec<T> codec) {
        ServerCustomPacketType<T> type = new ServerCustomPacketType<>(id, packetClass, codec);
        if (SERVERBOUND.putIfAbsent(id, type) != null) {
            throw new IllegalArgumentException("Duplicate serverbound custom packet: " + id);
        }
        return type;
    }

    public static ClientCustomPacketType<?> clientbound(String id) {
        return CLIENTBOUND.get(id);
    }

    public static ServerCustomPacketType<?> serverbound(String id) {
        return SERVERBOUND.get(id);
    }
}
