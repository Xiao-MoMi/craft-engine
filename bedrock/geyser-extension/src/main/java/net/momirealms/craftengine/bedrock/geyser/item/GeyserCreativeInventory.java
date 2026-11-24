package net.momirealms.craftengine.bedrock.geyser.item;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.ItemMappingsProxy;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeyserCreativeInventory {
    private static final int CATEGORY_COUNT = CreativeItemCategory.values().length;

    private final GeyserSession session;
    private final Map<String, CreativeItemCategory> categoriesByVariant;
    private final ExtensionLogger logger;
    private List<ItemStack> pendingJavaStacks = new ArrayList<>();
    private ItemMappings originalMappings;
    private ItemMappings creativeMappings;
    private boolean initialContentSeen;
    private boolean finishedReceivingStacks;
    private boolean closed;

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
        if (!this.finishedReceivingStacks) {
            return;
        }
        CreativeContentPacket content = this.buildCreativeContent();
        if (content != null) {
            event.setPacket(content);
        }
    }

    public void receive(boolean last, ByteBuffer stacks) {
        if (this.finishedReceivingStacks || this.closed) {
            return;
        }
        if (!this.readChunk(stacks)) {
            this.pendingJavaStacks = List.of();
            this.finishedReceivingStacks = true;
            return;
        }
        if (!last) {
            return;
        }
        this.finishedReceivingStacks = true;
        if (this.initialContentSeen) {
            CreativeContentPacket content = this.buildCreativeContent();
            if (content != null) {
                this.session.sendUpstreamPacket(content);
            }
        }
    }

    private boolean readChunk(ByteBuffer stacks) {
        ByteBuf chunk = Unpooled.wrappedBuffer(stacks);
        try {
            if (chunk.readableBytes() > BedrockProtocol.CREATIVE_ITEMS_CHUNK_BYTES) {
                throw new IllegalArgumentException("Creative item chunk exceeds its size limit");
            }
            while (chunk.isReadable()) {
                this.readStack(chunk);
            }
            return true;
        } catch (RuntimeException exception) {
            this.logger.error("Could not read CraftEngine creative items", exception);
            return false;
        } finally {
            chunk.release();
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
                this.pendingJavaStacks.add(item);
            }
        } catch (RuntimeException exception) {
            this.logger.error("Could not decode a CraftEngine creative item stack", exception);
        }
    }

    @Nullable
    private CreativeContentPacket buildCreativeContent() {
        List<ItemStack> javaStacks = this.pendingJavaStacks;
        this.pendingJavaStacks = List.of();
        if (javaStacks.isEmpty()) {
            return null;
        }
        ItemMappings baseMappings = this.session.getItemMappings();
        CreativeContentPacket content = new CreativeContentPacket();
        List<CreativeItemData> entries = content.getContents();
        List<CreativeItemGroup> groups = content.getGroups();
        entries.addAll(baseMappings.getCreativeItems());
        groups.addAll(baseMappings.getCreativeItemGroups());

        int[] unnamedGroupIds = groups.isEmpty() ? null : indexUnnamedGroups(groups);
        int expectedEntries = Math.min(javaStacks.size(), this.categoriesByVariant.size());
        Map<ItemData, ItemStack> javaStacksByEntry = new IdentityHashMap<>(expectedEntries);
        Map<ItemDefinition, ItemMapping> reverseMappings = LinkedHashMap.newLinkedHashMap(expectedEntries);
        for (ItemStack javaStack : javaStacks) {
            ItemData appearance = this.translateAppearance(javaStack);
            if (appearance == null) {
                continue;
            }
            ItemDefinition definition = appearance.getDefinition();
            CreativeItemCategory category = this.categoriesByVariant.get(definition.getIdentifier());
            if (category == null) {
                continue;
            }
            int groupId = creativeGroupId(groups, unnamedGroupIds, category);
            int netId = entries.size() + 1;
            ItemData data = appearance.toBuilder().netId(netId).build();
            entries.add(new CreativeItemData(data, netId, groupId));
            javaStacksByEntry.put(data, javaStack);
            reverseMappings.computeIfAbsent(definition, key -> ItemMapping.builder()
                    .bedrockIdentifier(key.getIdentifier())
                    .bedrockDefinition(key)
                    .bedrockBlockDefinition(data.getBlockDefinition())
                    .javaItem(new CreativeItem(key.getIdentifier(), javaStacksByEntry))
                    .build());
        }
        if (reverseMappings.isEmpty()) {
            return null;
        }
        this.installCreativeMappings(baseMappings, reverseMappings.values(), content);
        return content;
    }

    @Nullable
    private ItemData translateAppearance(ItemStack stack) {
        try {
            return ItemTranslator.translateToBedrock(this.session, stack);
        } catch (RuntimeException exception) {
            this.logger.error("Could not translate a CraftEngine creative item with Java ID " + stack.getId(), exception);
            return null;
        }
    }

    private static int[] indexUnnamedGroups(List<CreativeItemGroup> groups) {
        int[] indices = new int[CATEGORY_COUNT];
        Arrays.fill(indices, -1);
        for (int i = 0; i < groups.size(); i++) {
            CreativeItemGroup group = groups.get(i);
            if (group.getName().isEmpty()) {
                indices[group.getCategory().ordinal()] = i;
            }
        }
        return indices;
    }

    private static int creativeGroupId(List<CreativeItemGroup> groups, @Nullable int[] unnamedGroupIds, CreativeItemCategory category) {
        if (unnamedGroupIds == null) {
            return 0;
        }
        int index = category.ordinal();
        int groupId = unnamedGroupIds[index];
        if (groupId == -1) {
            groupId = groups.size();
            groups.add(new CreativeItemGroup(category, "", ItemData.AIR));
            unnamedGroupIds[index] = groupId;
        }
        return groupId;
    }

    private void installCreativeMappings(ItemMappings baseMappings, Collection<ItemMapping> reverseMappings, CreativeContentPacket content) {
        ItemMapping[] base = baseMappings.getItems();
        ItemMapping[] mappings = Arrays.copyOf(base, base.length + reverseMappings.size());
        int index = base.length;
        for (ItemMapping mapping : reverseMappings) {
            mappings[index++] = mapping;
        }
        List<CreativeItemData> entries = content.getContents();
        List<CreativeItemData> creativeItems = Collections.unmodifiableList(Arrays.asList(entries.toArray(new CreativeItemData[entries.size()])));
        this.originalMappings = baseMappings;
        this.creativeMappings = ItemMappingsProxy.INSTANCE.copy(baseMappings, mappings, creativeItems, List.copyOf(content.getGroups()));
        this.session.setItemMappings(this.creativeMappings);
    }

    public void close() {
        this.closed = true;
        this.pendingJavaStacks = List.of();
        if (this.creativeMappings != null && this.session.getItemMappings() == this.creativeMappings) {
            this.session.setItemMappings(this.originalMappings);
        }
    }

    private static final class CreativeItem extends Item {
        private final Map<ItemData, ItemStack> javaStacksByEntry;

        private CreativeItem(String identifier, Map<ItemData, ItemStack> javaStacksByEntry) {
            super(identifier, Item.builder());
            this.javaStacksByEntry = javaStacksByEntry;
        }

        @Override
        public boolean ignoreDamage() {
            return true;
        }

        @NotNull
        @Override
        public GeyserItemStack translateToJava(GeyserSession session, @NotNull ItemData data, @NotNull ItemMapping mapping, @NotNull ItemMappings mappings) {
            // Geyser resolves a creative request to the exact catalogue entry. Other packets may carry the same runtime ID.
            ItemStack template = this.javaStacksByEntry.get(data);
            if (template == null || session.getGameMode() != GameMode.CREATIVE) {
                return Items.AIR.translateToJava(session, data, mapping, mappings);
            }
            DataComponents components = template.getDataComponentsPatch();
            return GeyserItemStack.of(session, template.getId(), template.getAmount(), components == null ? null : components.clone());
        }
    }
}
