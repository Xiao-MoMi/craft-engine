package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.network.ClientCustomPacket;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

public record ClientboundNativeModelPacket(NativeModelPayload state) implements ClientCustomPacket {
    public static final PacketCodec<ClientboundNativeModelPacket> CODEC = PacketCodec.of(
            packet -> NativeModelPayload.CODEC.encode(packet.state),
            buffer -> new ClientboundNativeModelPacket(NativeModelPayload.CODEC.decode(buffer))
    );

    @Override
    public String id() {
        return NativeModelPayload.CHANNEL;
    }

    @Override
    public void handle(GeyserPlayer player) {
        CraftEngineGeyserExtension.instance().nativeModels().accept(player, this.state);
    }
}
