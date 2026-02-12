package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.item.processor.ArgumentsProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

@SuppressWarnings("DuplicatedCode")
public final class LegacyNetworkItemHandler implements NetworkItemHandler<ItemStack> {

    @Override
    public Optional<Item<ItemStack>> c2s(Item<ItemStack> wrapped) {
        boolean forceReturn = false;

        // 处理收纳袋
        Object bundleContents = wrapped.getExactTag("Items");
        if (bundleContents != null) {
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            for (Object tag : (Iterable<?>) bundleContents) {
                Object previousItem = FastNMS.INSTANCE.method$ItemStack$of(tag);
                Optional<ItemStack> itemStack = BukkitItemManager.instance().c2s(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(previousItem));
                if (itemStack.isPresent()) {
                    newItems.add(FastNMS.INSTANCE.field$CraftItemStack$handle(itemStack.get()));
                    changed = true;
                } else {
                    newItems.add(previousItem);
                }
            }
            if (changed) {
                Object listTag = FastNMS.INSTANCE.constructor$ListTag();
                for (Object newItem : newItems) {
                    FastNMS.INSTANCE.method$ListTag$add(listTag, 0, FastNMS.INSTANCE.method$itemStack$save(newItem, FastNMS.INSTANCE.constructor$CompoundTag()));
                }
                wrapped.setTag(listTag, "Items");
                forceReturn = true;
            }
        }

        // 处理container
        Object containerContents = wrapped.getExactTag("BlockEntityTag");
        if (containerContents != null) {
            Object itemTags = FastNMS.INSTANCE.method$CompoundTag$get(containerContents, "Items");
            if (itemTags != null) {
                boolean changed = false;
                List<Pair<Byte, Object>> newItems = new ArrayList<>();
                for (Object tag : (Iterable<?>) itemTags) {
                    Object previousItem = FastNMS.INSTANCE.method$ItemStack$of(tag);
                    Optional<ItemStack> itemStack = BukkitItemManager.instance().c2s(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(previousItem));
                    byte slot = FastNMS.INSTANCE.method$ByteTag$value(FastNMS.INSTANCE.method$CompoundTag$get(tag, "Slot"));
                    if (itemStack.isPresent()) {
                        newItems.add(Pair.of(slot, FastNMS.INSTANCE.field$CraftItemStack$handle(itemStack.get())));
                        changed = true;
                    } else {
                        newItems.add(Pair.of(slot, previousItem));
                    }
                }
                if (changed) {
                    Object listTag = FastNMS.INSTANCE.constructor$ListTag();
                    for (Pair<Byte, Object> newItem : newItems) {
                        Object newTag = FastNMS.INSTANCE.method$itemStack$save(newItem.right(), FastNMS.INSTANCE.constructor$CompoundTag());
                        Object slotTag = FastNMS.INSTANCE.constructor$ByteTag(newItem.left());
                        FastNMS.INSTANCE.method$CompoundTag$put(newTag, "Slot", slotTag);
                        FastNMS.INSTANCE.method$ListTag$add(listTag, 0, newTag);
                    }
                    wrapped.setTag(listTag, "BlockEntityTag", "Items");
                    forceReturn = true;
                }
            }
        }

        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
            if (customItem.item() != FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject())) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.item(), wrapped.count());
                forceReturn = true;
            }
        }

        CompoundTag networkData = (CompoundTag) wrapped.getTag(NETWORK_ITEM_TAG);
        if (networkData != null) {
            forceReturn = true;
            // 移除tag
            wrapped.removeTag(NETWORK_ITEM_TAG);
            // 恢复物品
            for (Map.Entry<String, Tag> entry : networkData.entrySet()) {
                if (entry.getValue() instanceof CompoundTag tag) {
                    NetworkItemHandler.apply(entry.getKey(), tag, wrapped);
                }
            }
        }

        return forceReturn ? Optional.of(wrapped) : Optional.empty();
    }

    @Override
    public Optional<Item<ItemStack>> s2c(Item<ItemStack> wrapped, @Nullable Player player) {
        boolean forceReturn = false;

        // 处理收纳袋
        Object bundleContents = wrapped.getExactTag("Items");
        if (bundleContents != null) {
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            for (Object tag : (Iterable<?>) bundleContents) {
                Object previousItem = FastNMS.INSTANCE.method$ItemStack$of(tag);
                Optional<ItemStack> itemStack = BukkitItemManager.instance().s2c(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(previousItem), player);
                if (itemStack.isPresent()) {
                    newItems.add(FastNMS.INSTANCE.field$CraftItemStack$handle(itemStack.get()));
                    changed = true;
                } else {
                    newItems.add(previousItem);
                }
            }
            if (changed) {
                Object listTag = FastNMS.INSTANCE.constructor$ListTag();
                for (Object newItem : newItems) {
                    FastNMS.INSTANCE.method$ListTag$add(listTag, 0, FastNMS.INSTANCE.method$itemStack$save(newItem, FastNMS.INSTANCE.constructor$CompoundTag()));
                }
                wrapped.setTag(listTag, "Items");
                forceReturn = true;
            }
        }

        // 处理container
        Object containerContents = wrapped.getExactTag("BlockEntityTag");
        if (containerContents != null) {
            Object itemTags = FastNMS.INSTANCE.method$CompoundTag$get(containerContents, "Items");
            if (itemTags != null) {
                boolean changed = false;
                List<Pair<Byte, Object>> newItems = new ArrayList<>();
                for (Object tag : (Iterable<?>) itemTags) {
                    Object previousItem = FastNMS.INSTANCE.method$ItemStack$of(tag);
                    Optional<ItemStack> itemStack = BukkitItemManager.instance().s2c(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(previousItem), player);
                    byte slot = FastNMS.INSTANCE.method$ByteTag$value(FastNMS.INSTANCE.method$CompoundTag$get(tag, "Slot"));
                    if (itemStack.isPresent()) {
                        newItems.add(Pair.of(slot, FastNMS.INSTANCE.field$CraftItemStack$handle(itemStack.get())));
                        changed = true;
                    } else {
                        newItems.add(Pair.of(slot, previousItem));
                    }
                }
                if (changed) {
                    Object listTag = FastNMS.INSTANCE.constructor$ListTag();
                    for (Pair<Byte, Object> newItem : newItems) {
                        Object newTag = FastNMS.INSTANCE.method$itemStack$save(newItem.right(), FastNMS.INSTANCE.constructor$CompoundTag());
                        Object slotTag = FastNMS.INSTANCE.constructor$ByteTag(newItem.left());
                        FastNMS.INSTANCE.method$CompoundTag$put(newTag, "Slot", slotTag);
                        FastNMS.INSTANCE.method$ListTag$add(listTag, 0, newTag);
                    }
                    wrapped.setTag(listTag, "BlockEntityTag", "Items");
                    forceReturn = true;
                }
            }
        }

        // todo 处理book

        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        // 不是自定义物品或修改过的原版物品
        if (optionalCustomItem.isEmpty()) {
            if (!Config.interceptItem()) {
                return forceReturn ? Optional.of(wrapped) : Optional.empty();
            }
            return new OtherItem(wrapped, forceReturn).process(NetworkTextReplaceContext.of(player));
        }

        // 应用 client-bound-material
        BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
        if (customItem.hasClientboundMaterial() && FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject()) != customItem.clientItem()) {
            wrapped = wrapped.unsafeTransmuteCopy(customItem.clientItem(), wrapped.count());
            forceReturn = true;
        }

        // 没有客户端侧组件
        if (!customItem.hasClientBoundDataModifier()) {
            if (!Config.interceptItem()) {
                return forceReturn ? Optional.of(wrapped) : Optional.empty();
            }
            return new OtherItem(wrapped, forceReturn).process(NetworkTextReplaceContext.of(player));
        }

        // 应用client-bound-data
        CompoundTag tag = new CompoundTag();
        // 创建context
        Tag argumentTag = wrapped.getTag(ArgumentsProcessor.ARGUMENTS_TAG);
        NetworkItemBuildContext context;
        if (argumentTag instanceof CompoundTag arguments) {
            ContextHolder.Builder builder = ContextHolder.builder();
            for (Map.Entry<String, Tag> entry : arguments.entrySet()) {
                builder.withParameter(ContextKey.direct(entry.getKey()), entry.getValue().getAsString());
            }
            context = NetworkItemBuildContext.of(player, builder);
        } else {
            context = NetworkItemBuildContext.of(player);
        }
        // 准备阶段
        for (ItemProcessor modifier : customItem.clientBoundDataModifiers()) {
            modifier.prepareNetworkItem(wrapped, context, tag);
        }
        // 如果拦截物品的描述名称等
        if (Config.interceptItem()) {
            if (wrapped.hasTag("display", "Name")) {
                processCustomName(wrapped, tag::put, context);
            }
            if (wrapped.hasTag("display", "Lore")) {
                processLore(wrapped, tag::put, context);
            }
        }
        // 应用阶段
        for (ItemProcessor modifier : customItem.clientBoundDataModifiers()) {
            modifier.apply(wrapped, context);
        }
        // 如果tag不空，则需要返回
        if (!tag.isEmpty()) {
            wrapped.setTag(tag, NETWORK_ITEM_TAG);
            forceReturn = true;
        }
        return forceReturn ? Optional.of(wrapped) : Optional.empty();
    }

    public static boolean processCustomName(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback, Context context) {
        Optional<String> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            String line = optionalCustomName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(line);
            if (!tokens.isEmpty()) {
                item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens, context)));
                callback.accept("display.Name", NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    private static boolean processLore(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback, Context context) {
        Optional<List<String>> optionalLore = item.loreJson();
        if (optionalLore.isPresent()) {
            boolean changed = false;
            List<String> lore = optionalLore.get();
            List<String> newLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(line);
                if (tokens.isEmpty()) {
                    newLore.add(line);
                } else {
                    newLore.add(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens, context)));
                    changed = true;
                }
            }
            if (changed) {
                item.loreJson(newLore);
                ListTag listTag = new ListTag();
                for (String line : lore) {
                    listTag.add(new StringTag(line));
                }
                callback.accept("display.Lore", NetworkItemHandler.pack(Operation.ADD, listTag));
                return true;
            }
        }
        return false;
    }

    static class OtherItem {
        private final Item<ItemStack> item;
        private boolean globalChanged = false;
        private CompoundTag networkTag;
        private final boolean forceReturn;

        public OtherItem(Item<ItemStack> item, boolean forceReturn) {
            this.item = item;
            this.forceReturn = forceReturn;
        }

        public Optional<Item<ItemStack>> process(Context context) {
            if (processLore(this.item, (s, c) -> networkTag().put(s, c), context)) {
                this.globalChanged = true;
            }
            if (processCustomName(this.item, (s, c) -> networkTag().put(s, c), context)) {
                this.globalChanged = true;
            }
            if (this.globalChanged) {
                this.item.setTag(this.networkTag, NETWORK_ITEM_TAG);
                return Optional.of(this.item);
            } else if (this.forceReturn) {
                return Optional.of(this.item);
            } else {
                return Optional.empty();
            }
        }

        public CompoundTag networkTag() {
            if (this.networkTag == null) {
                this.networkTag = new CompoundTag();
            }
            return this.networkTag;
        }
    }
}
