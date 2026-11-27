package net.momirealms.craftengine.bedrock.geyser.item;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.proxy.session.GeyserSessionProxy;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class GeyserCreativeInventory {
    private static final int ITEMS_PER_BATCH = 128;
    private final GeyserSession session;
    private final Map<String, CreativeItemCategory> categoriesByVariant;
    private final ExtensionLogger logger;
    private final List<ByteBuffer> chunks = new ArrayList<>();
    private int chunkIndex;
    private int chunkOffset;
    private boolean scheduled;
    private boolean finished;
    private GeyserCreativeCatalog catalog;
    private ItemMappings catalogBase;
    private Object catalogWorld;
    private String catalogLocale;
    private boolean receivingChunks = true;
    private boolean initialContentSeen;
    private ItemMappings originalMappings;
    private ItemMappings creativeMappings;

    GeyserCreativeInventory(GeyserSession session, Map<String, CreativeItemCategory> categoriesByVariant, ExtensionLogger logger) {
        this.session = session;
        this.categoriesByVariant = categoriesByVariant;
        this.logger = logger;
    }

    public void onCreativeContent(GeyserPacketEvent<CreativeContentPacket> event) {
        if (this.initialContentSeen) {
            return;
        }
        this.initialContentSeen = true;
        this.drain(event::setPacket);
    }

    public void receive(boolean last, ByteBuffer stacks) {
        if (!this.receivingChunks || this.finished) {
            return;
        }
        if (stacks.remaining() > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES) {
            this.logger.error("Creative item chunk exceeds its size limit");
            this.discard();
            return;
        }
        if (stacks.hasRemaining()) {
            this.chunks.add(stacks.slice().asReadOnlyBuffer());
        }
        this.receivingChunks = !last;
        this.drain(this.session::sendUpstreamPacket);
    }

    private void drain(Consumer<CreativeContentPacket> writer) {
        if (this.finished) {
            return;
        }
        if (this.session.isClosed() || (!this.receivingChunks && this.chunks.isEmpty())) {
            this.discard();
            return;
        }
        if (this.chunks.isEmpty()) {
            return;
        }
        ItemMappings base = this.session.getItemMappings();
        Object world = GeyserSessionProxy.INSTANCE.worldName(this.session);
        String locale = this.session.locale();
        if (base != this.catalogBase || !Objects.equals(world, this.catalogWorld) || !Objects.equals(locale, this.catalogLocale)) {
            // Rebuild against one mapping, world and locale if a transfer overlaps the handshake.
            this.catalog = new GeyserCreativeCatalog(base, this.categoriesByVariant.size());
            this.catalogBase = base;
            this.catalogWorld = world;
            this.catalogLocale = locale;
            this.chunkIndex = 0;
            this.chunkOffset = 0;
        }
        int budget = ITEMS_PER_BATCH;
        while (budget > 0 && this.chunkIndex < this.chunks.size()) {
            ByteBuffer encoded = this.chunks.get(this.chunkIndex).duplicate().position(this.chunkOffset);
            ByteBuf chunk = Unpooled.wrappedBuffer(encoded);
            try {
                while (budget > 0 && chunk.isReadable()) {
                    this.readStack(chunk);
                    budget--;
                }
                if (chunk.isReadable()) {
                    this.chunkOffset += chunk.readerIndex();
                } else {
                    this.chunkIndex++;
                    this.chunkOffset = 0;
                }
            } catch (RuntimeException exception) {
                this.logger.error("Could not read CraftEngine creative items", exception);
                this.discard();
                return;
            } finally {
                chunk.release();
            }
        }
        if (this.chunkIndex < this.chunks.size()) {
            if (!this.scheduled) {
                this.scheduled = true;
                this.session.executeInEventLoop(() -> {
                    this.scheduled = false;
                    this.drain(this.session::sendUpstreamPacket);
                });
            }
        } else if (!this.receivingChunks && this.initialContentSeen) {
            ItemMappings extended = this.catalog.finish(base);
            this.discard();
            if (extended != null) {
                this.originalMappings = base;
                this.creativeMappings = extended;
                this.session.setItemMappings(extended);
                CreativeContentPacket content = new CreativeContentPacket();
                content.getContents().addAll(extended.getCreativeItems());
                content.getGroups().addAll(extended.getCreativeItemGroups());
                writer.accept(content);
            }
        }
    }

    private void readStack(ByteBuf chunk) {
        int size = MinecraftTypes.readVarInt(chunk);
        if (size <= 0 || size > chunk.readableBytes()) {
            throw new IllegalArgumentException("Invalid creative item length " + size);
        }
        ByteBuf encoded = chunk.readSlice(size);
        try {
            ItemStack item = MinecraftTypes.readOptionalItemStack(encoded);
            if (item != null && !encoded.isReadable()) {
                this.catalog.append(this.session, item, this.categoriesByVariant, this.logger);
            }
        } catch (RuntimeException exception) {
            this.logger.error("Could not decode a CraftEngine creative item stack", exception);
        }
    }

    private void discard() {
        this.finished = true;
        this.receivingChunks = false;
        this.chunks.clear();
        this.catalog = null;
        this.catalogBase = null;
        this.catalogWorld = null;
        this.catalogLocale = null;
    }

    public void close() {
        this.discard();
        if (this.creativeMappings != null && this.session.getItemMappings() == this.creativeMappings) {
            this.session.setItemMappings(this.originalMappings);
        }
    }
}
