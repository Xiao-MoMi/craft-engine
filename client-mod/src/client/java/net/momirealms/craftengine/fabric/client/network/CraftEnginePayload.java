package net.momirealms.craftengine.fabric.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CraftEnginePayload(byte[] data) implements CustomPayload {
    public static final Identifier CRAFTENGINE_PAYLOAD = Identifier.of("craftengine", "payload");
    public static final Id<CraftEnginePayload> ID = new Id<>(CraftEnginePayload.CRAFTENGINE_PAYLOAD);
    public static final PacketCodec<PacketByteBuf, CraftEnginePayload> CODEC = PacketCodec.of(
            (payload, byteBuf) -> byteBuf.writeBytes(payload.data()),
            buf -> {
                int i = buf.readableBytes();
                byte[] data = new byte[i];
                buf.readBytes(data);
                return new CraftEnginePayload(data);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
