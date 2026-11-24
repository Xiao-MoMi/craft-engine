package net.momirealms.craftengine.bedrock.geyser.item;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
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
import java.util.function.Consumer;

public final class GeyserCreativeInventory {
    private final GeyserSession session;
    private final Map<String, CreativeItemCategory> categoriesByVariant;
    private final ExtensionLogger logger;
    private List<ItemStack> pendingStacks = new ArrayList<>();
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
        this.publishCatalogIfReady(event::setPacket);
    }

    public void receive(boolean last, ByteBuffer stacks) {
        if (!this.receivingChunks) {
            return;
        }
        ByteBuf chunk = Unpooled.wrappedBuffer(stacks);
        try {
            if (chunk.readableBytes() > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES) {
                throw new IllegalArgumentException("Creative item chunk exceeds its size limit");
            }
            while (chunk.isReadable()) {
                this.readStack(chunk);
            }
        } catch (RuntimeException exception) {
            this.logger.error("Could not read CraftEngine creative items", exception);
            this.receivingChunks = false;
            this.pendingStacks = List.of();
            return;
        } finally {
            chunk.release();
        }

        if (last) {
            this.receivingChunks = false;
            this.publishCatalogIfReady(this.session::sendUpstreamPacket);
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
                this.pendingStacks.add(item);
            }
        } catch (RuntimeException exception) {
            this.logger.error("Could not decode a CraftEngine creative item stack", exception);
        }
    }

    private void publishCatalogIfReady(Consumer<CreativeContentPacket> sendContent) {
        if (this.receivingChunks || !this.initialContentSeen || this.pendingStacks.isEmpty()) {
            return;
        }
        List<ItemStack> stacks = this.pendingStacks;
        this.pendingStacks = List.of();
        ItemMappings base = this.session.getItemMappings();
        ItemMappings extended = GeyserCreativeCatalog.build(this.session, base, stacks, this.categoriesByVariant, this.logger);
        if (extended == null) {
            return;
        }
        this.originalMappings = base;
        this.creativeMappings = extended;
        this.session.setItemMappings(extended);

        CreativeContentPacket content = new CreativeContentPacket();
        content.getContents().addAll(extended.getCreativeItems());
        content.getGroups().addAll(extended.getCreativeItemGroups());
        sendContent.accept(content);
    }

    public void close() {
        this.receivingChunks = false;
        this.pendingStacks = List.of();
        if (this.creativeMappings != null && this.session.getItemMappings() == this.creativeMappings) {
            this.session.setItemMappings(this.originalMappings);
        }
    }
}
