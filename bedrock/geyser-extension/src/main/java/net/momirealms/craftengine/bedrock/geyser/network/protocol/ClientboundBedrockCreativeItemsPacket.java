package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.item.GeyserCreativeInventory;
import net.momirealms.craftengine.bedrock.geyser.network.ClientCustomPacket;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

import java.nio.ByteBuffer;

public record ClientboundBedrockCreativeItemsPacket(boolean last, ByteBuffer stacks) implements ClientCustomPacket {
    public static final String ID = BedrockProtocol.CREATIVE_ITEMS_CHANNEL;
    public static final PacketCodec<ClientboundBedrockCreativeItemsPacket> CODEC = PacketCodec.of(
            packet -> ByteBuffer.allocate(1 + packet.stacks.remaining()).put((byte) (packet.last ? 1 : 0)).put(packet.stacks.duplicate()).array(),
            buffer -> new ClientboundBedrockCreativeItemsPacket(buffer.get() != 0, buffer.slice())
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void handle(GeyserPlayer player) {
        GeyserCreativeInventory inventory = player.creativeInventory();
        if (inventory != null) {
            player.session().executeInEventLoop(() -> inventory.receive(this.last, this.stacks));
        }
    }
}
