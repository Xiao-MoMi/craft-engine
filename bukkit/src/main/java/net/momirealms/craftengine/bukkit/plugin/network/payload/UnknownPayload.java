package net.momirealms.craftengine.bukkit.plugin.network.payload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerboundCustomPayloadPacketProxy;

public record UnknownPayload(Key channel, Object rawPayload) implements Payload {

    public static UnknownPayload from(Object payload) {
        Object id = ServerboundCustomPayloadPacketProxy.UnknownPayloadProxy.INSTANCE.getId(payload);
        return new UnknownPayload(KeyUtils.identifierToKey(id), payload);
    }

    public ByteBuf getData() {
        if (this.rawPayload instanceof ByteBuf buf) return buf;
        Object data = ServerboundCustomPayloadPacketProxy.UnknownPayloadProxy.INSTANCE.getData(this.rawPayload);
        if (data instanceof ByteBuf buf) return buf;
        if (data instanceof byte[] bytes) return Unpooled.wrappedBuffer(bytes);
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(this.getData());
    }
}
