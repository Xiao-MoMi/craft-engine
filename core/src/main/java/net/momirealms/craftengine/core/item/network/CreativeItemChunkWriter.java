package net.momirealms.craftengine.core.item.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientboundBedrockCreativeItemsPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Consumer;

final class CreativeItemChunkWriter implements AutoCloseable {
    private final Consumer<ClientboundBedrockCreativeItemsPacket> sendChunk;
    private final FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.buffer());

    CreativeItemChunkWriter(Consumer<ClientboundBedrockCreativeItemsPacket> sendChunk) {
        this.sendChunk = sendChunk;
    }

    boolean tryAppend(ByteBuf encodedStack) {
        int size = encodedStack.readableBytes();
        long framedSize = (long) FriendlyByteBuf.getVarIntSize(size) + size;
        if (framedSize > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES) {
            return false;
        }
        if (this.payload.readableBytes() + framedSize > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES) {
            this.flush(false);
        }
        this.payload.writeVarInt(size);
        this.payload.writeBytes(encodedStack);
        return true;
    }

    void flush(boolean last) {
        if (last || this.payload.isReadable()) {
            this.sendChunk.accept(new ClientboundBedrockCreativeItemsPacket(last, ByteBufUtil.getBytes(this.payload)));
            this.payload.clear();
        }
    }

    @Override
    public void close() {
        this.payload.release();
    }
}
