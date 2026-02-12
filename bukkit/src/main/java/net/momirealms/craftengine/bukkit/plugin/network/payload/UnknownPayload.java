package net.momirealms.craftengine.bukkit.plugin.network.payload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record UnknownPayload(Key channel, Object rawPayload) implements Payload {
    public static final boolean isByteArray = NetworkReflections.field$ServerboundCustomPayloadPacket$UnknownPayload$data == null;

    public static UnknownPayload from(Object payload) {
        try {
            Object id = NetworkReflections.field$ServerboundCustomPayloadPacket$UnknownPayload$id.get(payload);
            Key channel = KeyUtils.resourceLocationToKey(id);
            return new UnknownPayload(channel, payload);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to create UnknownPayload", e);
            return null;
        }
    }

    public ByteBuf getData() {
        if (this.rawPayload() instanceof ByteBuf buf) return buf;
        try {
            if (isByteArray) {
                return Unpooled.wrappedBuffer((byte[]) NetworkReflections.field$ServerboundCustomPayloadPacket$UnknownPayload$dataByteArray.get(this.rawPayload()));
            } else {
                return (ByteBuf) NetworkReflections.field$ServerboundCustomPayloadPacket$UnknownPayload$data.get(this.rawPayload());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to get data from UnknownPayload", e);
            return Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(this.getData());
    }
}
