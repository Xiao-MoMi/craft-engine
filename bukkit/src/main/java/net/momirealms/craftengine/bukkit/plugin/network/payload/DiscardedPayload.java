package net.momirealms.craftengine.bukkit.plugin.network.payload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom.CustomPacketPayloadProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom.DiscardedPayloadProxy;

public record DiscardedPayload(Key channel, Object rawPayload) implements Payload {

    public static DiscardedPayload from(Object payload) {
        Object type = CustomPacketPayloadProxy.INSTANCE.type(payload);
        Object id = CustomPacketPayloadProxy.TypeProxy.INSTANCE.getId(type);
        Key channel = KeyUtils.identifierToKey(id);
        return new DiscardedPayload(channel, payload);
    }

    public ByteBuf getData() {
        Object data = DiscardedPayloadProxy.INSTANCE.getData(this.rawPayload);
        if (data instanceof byte[] bytes) {
            return Unpooled.wrappedBuffer(bytes);
        } else {
            return (ByteBuf) data;
        }
    }

    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(this.getData());
    }
}
