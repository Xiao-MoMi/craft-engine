package net.momirealms.craftengine.core.item.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientboundBedrockCreativeItemsPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.function.Consumer;

public final class BedrockCreativeItemSender {

    private BedrockCreativeItemSender() {
    }

    public static void send(Player player, int clientProtocol) {
        if (!Config.bedrockCreativeInventoryPacketEnabled() || clientProtocol < 0 || !VersionHelper.isOrAbove1_21_4) {
            return;
        }
        CraftEngine plugin = CraftEngine.instance();
        int serverProtocol = VersionHelper.JAVA_PROTOCOL_VERSION;
        Consumer<ClientboundBedrockCreativeItemsPacket> sendChunk;
        if (clientProtocol == serverProtocol) {
            sendChunk = player::sendCustomPacket;
        } else {
            ClientboundItemRewriter rewriter = plugin.compatibilityManager().getViaVersionClientboundItemRewriter(player, serverProtocol, clientProtocol);
            if (rewriter == null) {
                return;
            }
            // ViaVersion reads connection state on the channel's event loop.
            sendChunk = packet -> player.nettyChannel().eventLoop().execute(() -> rewriteAndSend(player, rewriter, packet));
        }

        Runnable build = () -> sendItems(plugin, player, sendChunk);
        if (Config.bedrockAsyncCreativePacketBuild()) {
            plugin.scheduler().executeAsync(build);
        } else {
            plugin.scheduler().platform().run(build, null, player);
        }
    }

    private static void sendItems(CraftEngine plugin, Player player, Consumer<ClientboundBedrockCreativeItemsPacket> sendChunk) {
        ItemManager manager = plugin.itemManager();
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try (CreativeItemChunkWriter chunks = new CreativeItemChunkWriter(sendChunk)) {
            for (Key id : manager.bedrockCreativeItemIds()) {
                encoded.clear();
                try {
                    Item item = Item.byId(id, player);
                    if (item == null || item.isEmpty()) {
                        continue;
                    }
                    Item clientItem = manager.s2c(item, player, ItemPacketSource.GENERIC).orElse(item);
                    plugin.platform().writeItem(encoded, clientItem);
                } catch (RuntimeException exception) {
                    plugin.logger().warn("Could not encode Bedrock creative item " + id, exception);
                    continue;
                }
                if (encoded.isReadable() && !chunks.tryAppend(encoded)) {
                    plugin.logger().warn("Bedrock creative item exceeds the packet size limit: " + id);
                }
            }
            chunks.flush(true);
        } finally {
            encoded.release();
        }
    }

    private static void rewriteAndSend(Player player, ClientboundItemRewriter rewriter, ClientboundBedrockCreativeItemsPacket packet) {
        if (!player.nettyChannel().isActive()) {
            return;
        }
        FriendlyByteBuf source = new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.stacks()));
        ByteBuf translated = Unpooled.buffer();
        try (CreativeItemChunkWriter chunks = new CreativeItemChunkWriter(player::sendCustomPacket)) {
            while (source.isReadable()) {
                int size = source.readVarInt();
                ByteBuf stack = source.readSlice(size);
                translated.clear();
                try {
                    rewriter.rewrite(stack, translated);
                } catch (RuntimeException exception) {
                    CraftEngine.instance().logger().warn("Could not translate a Bedrock creative item with ViaVersion", exception);
                    continue;
                }
                if (translated.isReadable() && !chunks.tryAppend(translated)) {
                    CraftEngine.instance().logger().warn("Translated Bedrock creative item exceeds the packet size limit");
                }
            }
            chunks.flush(packet.last());
        } finally {
            source.release();
            translated.release();
        }
    }
}
