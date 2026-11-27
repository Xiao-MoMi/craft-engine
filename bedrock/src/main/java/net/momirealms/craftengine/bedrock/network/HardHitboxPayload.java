package net.momirealms.craftengine.bedrock.network;

import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

import java.nio.ByteBuffer;

public record HardHitboxPayload(int entityId, double width, double height) {
    public static final String CHANNEL = "craftengine:hard_hitbox";
    public static final PacketCodec<HardHitboxPayload> CODEC = PacketCodec.of(
            packet -> ByteBuffer.allocate(Integer.BYTES + Double.BYTES * 2)
                    .putInt(packet.entityId)
                    .putDouble(packet.width)
                    .putDouble(packet.height).array(),
            buffer -> new HardHitboxPayload(buffer.getInt(), buffer.getDouble(), buffer.getDouble())
    );
}