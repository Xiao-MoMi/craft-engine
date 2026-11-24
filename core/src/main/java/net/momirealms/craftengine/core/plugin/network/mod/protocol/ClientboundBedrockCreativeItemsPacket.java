package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.network.ClientboundItemRewriter;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record ClientboundBedrockCreativeItemsPacket(boolean last, byte[] stacks) implements ClientCustomPacket {
    public static final Key ID = Key.of(BedrockProtocol.CREATIVE_ITEMS_CHANNEL);
    public static final NetworkCodec<FriendlyByteBuf, ClientboundBedrockCreativeItemsPacket> CODEC = ClientCustomPacket.codec(
            (packet, buf) -> {
                buf.writeBoolean(packet.last);
                buf.writeBytes(packet.stacks);
            },
            buf -> new ClientboundBedrockCreativeItemsPacket(buf.readBoolean(), ByteBufUtil.getBytes(buf))
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundBedrockCreativeItemsPacket> codec() {
        return CODEC;
    }

    static void send(Player player, @Nullable ClientboundItemRewriter rewriter) {
        ItemManager manager = CraftEngine.instance().itemManager();
        Consumer<ClientboundBedrockCreativeItemsPacket> sender = rewriter == null
                ? player::sendCustomPacket
                : packet -> player.nettyChannel().eventLoop().execute(() -> packet.rewriteAndSend(player, rewriter));
        FriendlyByteBuf stack = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf chunk = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (Key id : manager.orderedItemIds()) {
                if (manager.isVanillaItem(id)) {
                    continue;
                }
                stack.clear();
                try {
                    Item item = Item.byId(id, player);
                    if (item == null || item.isEmpty()) {
                        continue;
                    }
                    item = manager.s2c(item, player, ItemPacketSource.GENERIC).orElse(item);
                    CraftEngine.instance().platform().writeItem(stack, item);
                } catch (RuntimeException exception) {
                    CraftEngine.instance().logger().warn("Could not encode Bedrock creative item " + id, exception);
                    continue;
                }
                if (stack.readableBytes() > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES - 5) {
                    CraftEngine.instance().logger().warn("Bedrock creative item exceeds the packet size limit: " + id);
                    continue;
                }
                writeStack(chunk, stack, sender);
            }
            sender.accept(new ClientboundBedrockCreativeItemsPacket(true, ByteBufUtil.getBytes(chunk)));
        } finally {
            stack.release();
            chunk.release();
        }
    }

    private void rewriteAndSend(Player player, ClientboundItemRewriter rewriter) {
        if (!player.nettyChannel().isActive()) {
            return;
        }
        FriendlyByteBuf source = new FriendlyByteBuf(Unpooled.wrappedBuffer(this.stacks));
        ByteBuf stack = Unpooled.buffer();
        FriendlyByteBuf chunk = new FriendlyByteBuf(Unpooled.buffer());
        Consumer<ClientboundBedrockCreativeItemsPacket> sender = player::sendCustomPacket;
        try {
            while (source.isReadable()) {
                ByteBuf encoded = source.readSlice(source.readVarInt());
                stack.clear();
                try {
                    rewriter.rewrite(encoded, stack);
                } catch (RuntimeException exception) {
                    CraftEngine.instance().logger().warn("Could not translate a Bedrock creative item with ViaVersion", exception);
                    continue;
                }
                if (stack.readableBytes() > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES - 5) {
                    CraftEngine.instance().logger().warn("Translated Bedrock creative item exceeds the packet size limit");
                    continue;
                }
                if (stack.isReadable()) {
                    writeStack(chunk, stack, sender);
                }
            }
            if (this.last || chunk.isReadable()) {
                sender.accept(new ClientboundBedrockCreativeItemsPacket(this.last, ByteBufUtil.getBytes(chunk)));
            }
        } finally {
            source.release();
            stack.release();
            chunk.release();
        }
    }

    private static void writeStack(FriendlyByteBuf chunk, ByteBuf stack, Consumer<ClientboundBedrockCreativeItemsPacket> sender) {
        int size = stack.readableBytes();
        if (chunk.readableBytes() + size + 5 > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES) {
            sender.accept(new ClientboundBedrockCreativeItemsPacket(false, ByteBufUtil.getBytes(chunk)));
            chunk.clear();
        }
        chunk.writeVarInt(size);
        chunk.writeBytes(stack);
    }
}
