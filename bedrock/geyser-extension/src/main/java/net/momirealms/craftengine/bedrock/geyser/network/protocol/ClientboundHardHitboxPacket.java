package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.network.ClientCustomPacket;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.HardHitboxPayload;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

public record ClientboundHardHitboxPacket(HardHitboxPayload hitbox) implements ClientCustomPacket {
    public static final String ID = HardHitboxPayload.CHANNEL;
    public static final PacketCodec<ClientboundHardHitboxPacket> CODEC = PacketCodec.of(
            packet -> HardHitboxPayload.CODEC.encode(packet.hitbox),
            buffer -> new ClientboundHardHitboxPacket(HardHitboxPayload.CODEC.decode(buffer))
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void handle(GeyserPlayer player) {
        player.queueHitbox(this.hitbox);
    }
}
