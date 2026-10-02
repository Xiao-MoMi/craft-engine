package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import io.netty.handler.codec.DecoderException;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.gui.category.Category;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 用服务端物品浏览器的分类覆盖客户端创造模式标签页的分类状态。
 * 必须紧跟在同一批 {@link ClientboundCreativeModeTabItemsPacket} 之后发送，条目通过下标引用该批物品列表。
 * 客户端只以纯文本显示标题，因此标题以去除颜色与样式的纯文本发送。
 */
public record ClientboundCreativeModeTabCategoriesPacket(List<TabCategory> categories) implements ClientCustomPacket {
    public static final Key ID = Key.ce("creative_mode_tab_categories");
    public static final int FORMAT_VERSION = 1;
    // 原版自定义负载包的上限为 1MiB，超过 90% 时发出警告
    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024;
    private static final int WARN_PAYLOAD_SIZE = MAX_PAYLOAD_SIZE / 10 * 9;
    public static final NetworkCodec<FriendlyByteBuf, ClientboundCreativeModeTabCategoriesPacket> CODEC = ClientCustomPacket.codec(
            ClientboundCreativeModeTabCategoriesPacket::encode,
            ClientboundCreativeModeTabCategoriesPacket::decode
    );

    private static ClientboundCreativeModeTabCategoriesPacket decode(FriendlyByteBuf buf) {
        int formatVersion = buf.readVarInt();
        if (formatVersion != FORMAT_VERSION) {
            throw new DecoderException("Unsupported creative mode tab categories format version " + formatVersion);
        }
        int categoryCount = buf.readVarInt();
        List<TabCategory> categories = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            Key id = Key.of(buf.readUtf());
            String title = buf.readUtf();
            Item icon = buf.readBoolean() ? CraftEngine.instance().platform().readItem(buf) : null;
            int entryCount = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(entryCount);
            for (int j = 0; j < entryCount; j++) {
                int ref = buf.readVarInt();
                entries.add(ref == 0 ? Entry.inline(CraftEngine.instance().platform().readItem(buf)) : new Entry(ref, null));
            }
            categories.add(new TabCategory(id, title, icon, entries));
        }
        return new ClientboundCreativeModeTabCategoriesPacket(categories);
    }

    private void encode(FriendlyByteBuf buf) {
        int start = buf.writerIndex();
        buf.writeVarInt(FORMAT_VERSION);
        buf.writeVarInt(this.categories.size());
        for (TabCategory category : this.categories) {
            buf.writeUtf(category.id().asString());
            buf.writeUtf(category.title());
            buf.writeBoolean(category.icon() != null);
            if (category.icon() != null) {
                CraftEngine.instance().platform().writeItem(buf, category.icon());
            }
            buf.writeVarInt(category.entries().size());
            for (Entry entry : category.entries()) {
                buf.writeVarInt(entry.ref());
                if (entry.ref() == 0) {
                    CraftEngine.instance().platform().writeItem(buf, entry.item());
                }
            }
        }
        int size = buf.writerIndex() - start;
        if (size >= WARN_PAYLOAD_SIZE) {
            CraftEngine.instance().logger().warn("Creative mode tab categories payload is " + size + " bytes, which is close to the " + MAX_PAYLOAD_SIZE + " bytes limit. Consider reducing the number of categories or inline (vanilla) items.");
        }
    }

    /**
     * @param player      接收者
     * @param itemIndexes 同一批 creative_mode_tab_items 包（SET + ADD 累积）中物品 id 到最终下标的映射
     */
    public static ClientboundCreativeModeTabCategoriesPacket create(@NotNull Player player, @NotNull Map<Key, Integer> itemIndexes) {
        CraftEngine plugin = CraftEngine.instance();
        ItemBrowserManager itemBrowserManager = plugin.itemBrowserManager();
        ItemManager itemManager = plugin.itemManager();
        List<TabCategory> categories = new ArrayList<>();
        for (Category category : itemBrowserManager.categories()) {
            if (!category.creativeTab()) continue;
            if (!category.condition().test(PlayerOptionalContext.of(player))) continue;
            List<Entry> entries = new ArrayList<>();
            collectEntries(itemBrowserManager, itemManager, player, category, itemIndexes, new HashSet<>(), new HashSet<>(), entries);
            if (entries.isEmpty()) continue;
            categories.add(new TabCategory(category.id(), title(plugin, player, category), icon(itemManager, player, category), entries));
        }
        return new ClientboundCreativeModeTabCategoriesPacket(categories);
    }

    private static void collectEntries(ItemBrowserManager itemBrowserManager, ItemManager itemManager, Player player, Category category,
                                       Map<Key, Integer> itemIndexes, Set<Key> visitedCategories, Set<Key> addedItems, List<Entry> entries) {
        if (!visitedCategories.add(category.id())) return;
        for (String member : category.members()) {
            if (member.isEmpty()) continue;
            if (member.charAt(0) == '#') {
                Key subCategoryId = Key.of(member.substring(1));
                Optional<Category> subCategory = itemBrowserManager.byId(subCategoryId);
                if (subCategory.isEmpty()) {
                    Debugger.ITEM.debug(() -> "Cannot find sub category " + subCategoryId + " in category " + category.id() + " for creative mode tab");
                    continue;
                }
                if (!subCategory.get().condition().test(PlayerOptionalContext.of(player))) continue;
                collectEntries(itemBrowserManager, itemManager, player, subCategory.get(), itemIndexes, visitedCategories, addedItems, entries);
            } else {
                Key itemId = Key.of(member);
                if (!addedItems.add(itemId)) continue;
                Integer index = itemIndexes.get(itemId);
                if (index != null) {
                    entries.add(Entry.ref(index));
                    continue;
                }
                Item item = Item.byId(itemId, player);
                if (ItemUtils.isEmpty(item)) {
                    Debugger.ITEM.debug(() -> "Cannot find item " + itemId + " in category " + category.id() + " for creative mode tab");
                    continue;
                }
                entries.add(Entry.inline(itemManager.s2c(item, player, ItemPacketSource.GENERIC).orElse(item)));
            }
        }
    }

    // 与物品浏览器一致：先用空上下文解析（l10n 等网络标签保留为文本），再按玩家替换网络标签，最后去除样式转为纯文本
    private static String title(CraftEngine plugin, Player player, Category category) {
        Component title = AdventureHelper.miniMessage().deserialize(category.displayName(), ItemBuildContext.empty());
        Map<String, ComponentProvider> tokens = plugin.networkManager().matchNetworkTags(AdventureHelper.componentToJson(title));
        if (!tokens.isEmpty()) {
            title = AdventureHelper.replaceText(title, tokens, NetworkTextReplaceContext.of(player));
        }
        String plain = AdventureHelper.plainTextContent(title);
        // 仅含翻译键等非文本组件时纯文本为空，回退为分类 id
        return plain.isEmpty() ? category.id().asString() : plain;
    }

    @Nullable
    private static Item icon(ItemManager itemManager, Player player, Category category) {
        Item icon = Item.byId(category.icon(), player);
        if (ItemUtils.isEmpty(icon)) return null;
        return itemManager.s2c(icon, player, ItemPacketSource.GENERIC).orElse(icon);
    }

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundCreativeModeTabCategoriesPacket> codec() {
        return CODEC;
    }

    public record TabCategory(Key id, String title, @Nullable Item icon, List<Entry> entries) {
    }

    /**
     * @param ref  大于 0 时为物品列表下标 + 1，等于 0 时使用 {@link #item()}
     * @param item 仅在 ref 为 0 时存在
     */
    public record Entry(int ref, @Nullable Item item) {

        public static Entry ref(int index) {
            return new Entry(index + 1, null);
        }

        public static Entry inline(@NotNull Item item) {
            return new Entry(0, item);
        }
    }
}
