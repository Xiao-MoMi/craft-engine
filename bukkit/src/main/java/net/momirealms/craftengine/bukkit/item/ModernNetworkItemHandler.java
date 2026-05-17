package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.component.DataComponentIds;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.item.network.encrypt.ItemCrypto;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.BundleContentsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.ItemContainerContentsProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("DuplicatedCode")
public final class ModernNetworkItemHandler implements NetworkItemHandler {
    private final BukkitItemManager itemManager;

    public ModernNetworkItemHandler(BukkitItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public Optional<Item> c2s(Item wrapped) {
        boolean forceReturn = false;

        // 处理收纳袋
        if (wrapped.hasComponent(DataComponentTypes.BUNDLE_CONTENTS)) {
            Object bundleContents = wrapped.getExactComponent(DataComponentTypes.BUNDLE_CONTENTS);
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            if (VersionHelper.isOrAbove26_1) {
                for (Object itemTemplate : BundleContentsProxy.INSTANCE.getItems(bundleContents)) {
                    Object previousItem = ItemStackTemplateProxy.INSTANCE.create(itemTemplate);
                    Optional<Item> converted = this.itemManager.c2s(this.itemManager.wrap(previousItem));
                    if (converted.isPresent()) {
                        newItems.add(ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(converted.get().minecraftItem()));
                        changed = true;
                    } else {
                        newItems.add(itemTemplate);
                    }
                }
            } else {
                for (Object previousItem : BundleContentsProxy.INSTANCE.getItems(bundleContents)) {
                    Optional<Item> itemStack = this.itemManager.c2s(this.itemManager.wrap(previousItem));
                    if (itemStack.isPresent()) {
                        newItems.add(itemStack.get().minecraftItem());
                        changed = true;
                    } else {
                        newItems.add(previousItem);
                    }
                }
            }
            if (changed) {
                wrapped.setExactComponent(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsProxy.INSTANCE.newInstance(newItems));
                forceReturn = true;
            }
        }

        // 处理潜影盒等
        if (wrapped.hasComponent(DataComponentTypes.CONTAINER)) {
            Object containerContents = wrapped.getExactComponent(DataComponentTypes.CONTAINER);
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            if (VersionHelper.isOrAbove26_1) {
                for (Object previousItem : ItemContainerContentsProxy.INSTANCE.getItems(containerContents)) {
                    @SuppressWarnings("unchecked")
                    Optional<Object> previousTemplate = (Optional<Object>) previousItem;
                    if (previousTemplate.isPresent()) {
                        Object itemTemplate = previousTemplate.get();
                        BukkitItem wrap = this.itemManager.wrap(ItemStackTemplateProxy.INSTANCE.create(itemTemplate));
                        Optional<Item> converted = this.itemManager.c2s(wrap);
                        if (converted.isPresent()) {
                            newItems.add(converted.get().minecraftItem());
                            changed = true;
                        } else {
                            newItems.add(wrap.minecraftItem());
                        }
                    } else {
                        newItems.add(ItemStackProxy.EMPTY);
                    }
                }
            } else {
                for (Object previousItem : ItemContainerContentsProxy.INSTANCE.getItems(containerContents)) {
                    Optional<Item> converted = this.itemManager.c2s(this.itemManager.wrap(previousItem));
                    if (converted.isPresent()) {
                        newItems.add(converted.get().minecraftItem());
                        changed = true;
                    } else {
                        newItems.add(previousItem);
                    }
                }
            }
            if (changed) {
                wrapped.setExactComponent(DataComponentTypes.CONTAINER, ItemContainerContentsProxy.INSTANCE.fromItems(newItems));
                forceReturn = true;
            }
        }

        CompoundTag networkData = null;

        // 获取custom data
        CompoundTag customData = (CompoundTag) wrapped.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            // 先解密
            networkData = ItemCrypto.decrypt(customData.get(NETWORK_ITEM_TAG));
            if (networkData != null) {
                CompoundTag rawCustomDataTag = networkData.getCompound(DataComponentIds.CUSTOM_DATA);
                // 解密结果里可能有物品id，所以先还原 custom_data
                if (rawCustomDataTag != null) {
                    networkData.remove(DataComponentIds.CUSTOM_DATA);
                    NetworkItemHandler.apply(DataComponentIds.CUSTOM_DATA, rawCustomDataTag, wrapped);
                    customData = rawCustomDataTag.getCompound(NetworkItemHandler.NETWORK_VALUE);
                }
            }
        }

        Optional<ItemDefinition> itemDefinition = wrapped.getDefinition();
        // 一定要先尝试恢复client-bound-material，再应用组件变化
        if (itemDefinition.isPresent()) {
            BukkitItemDefinition customItem = (BukkitItemDefinition) itemDefinition.get();
            if (customItem.item() != ItemStackProxy.INSTANCE.getItem(wrapped.minecraftItem())) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.item(), wrapped.count());
                forceReturn = true;
            }
        }

        // 应用组件变化
        if (networkData != null) {
            forceReturn = true;
            // 移除网络 tag
            customData.remove(NETWORK_ITEM_TAG);

            // 恢复物品
            for (Map.Entry<String, Tag> entry : networkData.entrySet()) {
                if (entry.getValue() instanceof CompoundTag tag) {
                    NetworkItemHandler.apply(entry.getKey(), tag, wrapped);
                }
            }

            // 如果清空了，则直接移除这个组件
            if (customData.isEmpty()) wrapped.resetComponent(DataComponentTypes.CUSTOM_DATA);
            // 否则设置为新的
            else wrapped.setSparrowTagComponent(DataComponentTypes.CUSTOM_DATA, customData);
        }

        return forceReturn ? Optional.of(wrapped) : Optional.empty();
    }

    @Override
    public Optional<Item> s2c(Item wrapped, @Nullable Player player) {
        boolean forceReturn = false;

        // 处理收纳袋
        if (wrapped.hasComponent(DataComponentTypes.BUNDLE_CONTENTS)) {
            Object bundleContents = wrapped.getExactComponent(DataComponentTypes.BUNDLE_CONTENTS);
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            if (VersionHelper.isOrAbove26_1) {
                for (Object itemTemplate : BundleContentsProxy.INSTANCE.getItems(bundleContents)) {
                    Object previousItem = ItemStackTemplateProxy.INSTANCE.create(itemTemplate);
                    Optional<Item> converted = this.itemManager.s2c(this.itemManager.wrap(previousItem), player);
                    if (converted.isPresent()) {
                        newItems.add(ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(converted.get().minecraftItem()));
                        changed = true;
                    } else {
                        newItems.add(itemTemplate);
                    }
                }
            } else {
                for (Object previousItem : BundleContentsProxy.INSTANCE.getItems(bundleContents)) {
                    Optional<Item> converted = this.itemManager.s2c(this.itemManager.wrap(previousItem).copy(), player);
                    if (converted.isPresent()) {
                        newItems.add(converted.get().minecraftItem());
                        changed = true;
                    } else {
                        newItems.add(previousItem);
                    }
                }
            }
            if (changed) {
                wrapped.setExactComponent(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsProxy.INSTANCE.newInstance(newItems));
                forceReturn = true;
            }
        }

        // 处理潜影盒等
        if (wrapped.hasComponent(DataComponentTypes.CONTAINER)) {
            Object containerContents = wrapped.getExactComponent(DataComponentTypes.CONTAINER);
            boolean changed = false;
            List<Object> newItems = new ArrayList<>();
            if (VersionHelper.isOrAbove26_1) {
                for (Object optionalTemplate : ItemContainerContentsProxy.INSTANCE.getItems(containerContents)) {
                    @SuppressWarnings("unchecked")
                    Optional<Object> previousTemplate = (Optional<Object>) optionalTemplate;
                    if (previousTemplate.isPresent()) {
                        Object itemTemplate = previousTemplate.get();
                        Object previousItem = ItemStackTemplateProxy.INSTANCE.create(itemTemplate);
                        Optional<Item> converted = this.itemManager.s2c(this.itemManager.wrap(previousItem), player);
                        if (converted.isPresent()) {
                            newItems.add(converted.get().minecraftItem());
                            changed = true;
                        } else {
                            newItems.add(previousItem);
                        }
                    } else {
                        newItems.add(ItemStackProxy.EMPTY);
                    }
                }
            } else {
                for (Object previousItem : ItemContainerContentsProxy.INSTANCE.getItems(containerContents)) {
                    Optional<Item> itemStack = this.itemManager.s2c(this.itemManager.wrap(previousItem).copy(), player);
                    if (itemStack.isPresent()) {
                        newItems.add(itemStack.get().minecraftItem());
                        changed = true;
                    } else {
                        newItems.add(previousItem);
                    }
                }
            }
            if (changed) {
                wrapped.setExactComponent(DataComponentTypes.CONTAINER, ItemContainerContentsProxy.INSTANCE.fromItems(newItems));
                forceReturn = true;
            }
        }

        // 不是自定义物品或修改过的原版物品
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) {
            if (!Config.interceptItem()) {
                return forceReturn ? Optional.of(wrapped) : Optional.empty();
            }
            return new OtherItem(wrapped, forceReturn).process(NetworkTextReplaceContext.of(player));
        }

        BukkitItemDefinition customItem = (BukkitItemDefinition) optionalCustomItem.get();
        // 提前复制，这和物品类型相关
        Item original = wrapped;
        // 应用 client-bound-material前提是服务端侧物品类型和客户端侧的不同
        if (customItem.hasClientboundMaterial() && ItemStackProxy.INSTANCE.getItem(wrapped.minecraftItem()) != customItem.clientItem()) {
            wrapped = wrapped.unsafeTransmuteCopy(customItem.clientItem(), wrapped.count());
            forceReturn = true;
        }
        // 没有 client-bound-data
        if (!customItem.hasClientBoundProcessor()) {
            if (!Config.interceptItem()) {
                return forceReturn ? Optional.of(wrapped) : Optional.empty();
            }
            return new OtherItem(wrapped, forceReturn).process(NetworkTextReplaceContext.of(player));
        }
        // 创建context
        NetworkItemBuildContext context = NetworkItemBuildContext.of(player);
        // 准备阶段
        CompoundTag tag = new CompoundTag();
        for (ItemProcessor modifier : customItem.clientBoundProcessors()) {
            modifier.prepareNetworkItem(original, context, tag);
        }
        // 如果拦截物品的描述名称等
        if (Config.interceptItem()) {
            if (wrapped.hasComponent(DataComponentTypes.ITEM_NAME)) {
                if (VersionHelper.isOrAbove1_21_5) processModernItemName(wrapped, () -> tag, context);
                else processLegacyItemName(wrapped, () -> tag, context);
            }
            if (wrapped.hasComponent(DataComponentTypes.CUSTOM_NAME)) {
                if (VersionHelper.isOrAbove1_21_5) processModernCustomName(wrapped, () -> tag, context);
                else processLegacyCustomName(wrapped, () -> tag, context);
            }
            if (wrapped.hasComponent(DataComponentTypes.LORE)) {
                if (VersionHelper.isOrAbove1_21_5) processModernLore(wrapped, () -> tag, context);
                else processLegacyLore(wrapped, () -> tag, context);
            }
        }
        // 应用阶段
        for (ItemProcessor modifier : customItem.clientBoundProcessors()) {
            modifier.apply(wrapped, context);
        }
        // 如果tag不空，则需要返回
        if (!tag.isEmpty()) {
            CompoundTag customData = Optional.ofNullable(wrapped.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_DATA))
                    .map(CompoundTag.class::cast)
                    .orElseGet(CompoundTag::new);
            customData.put(NETWORK_ITEM_TAG, ItemCrypto.encrypt(tag));
            wrapped.setSparrowTagComponent(DataComponentTypes.CUSTOM_DATA, customData);
            forceReturn = true;
        }
        return forceReturn ? Optional.of(wrapped) : Optional.empty();
    }

    public static boolean processLegacyLore(Item item, Supplier<CompoundTag> tag, Context context) {
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
                tag.get().put(DataComponentIds.LORE, NetworkItemHandler.pack(Operation.ADD, listTag));
                return true;
            }
        }
        return false;
    }
    
    public static boolean processLegacyCustomName(Item item, Supplier<CompoundTag> tag, Context context) {
        Optional<String> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            String line = optionalCustomName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(line);
            if (!tokens.isEmpty()) {
                item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens, context)));
                tag.get().put(DataComponentIds.CUSTOM_NAME, NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    public static boolean processLegacyItemName(Item item, Supplier<CompoundTag> tag, Context context) {
        Optional<String> optionalItemName = item.itemNameJson();
        if (optionalItemName.isPresent()) {
            String line = optionalItemName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(line);
            if (!tokens.isEmpty()) {
                item.itemNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens, context)));
                tag.get().put(DataComponentIds.ITEM_NAME, NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    public static boolean processModernItemName(Item item, Supplier<CompoundTag> tag, Context context) {
        Tag nameTag = item.getComponentAsSparrowTag(DataComponentTypes.ITEM_NAME);
        if (nameTag == null) return false;
        Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(nameTag);
        if (!tokens.isEmpty()) {
            item.setSparrowTagComponent(DataComponentKeys.ITEM_NAME, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(nameTag), tokens, context)));
            tag.get().put(DataComponentIds.ITEM_NAME, NetworkItemHandler.pack(Operation.ADD, nameTag));
            return true;
        }
        return false;
    }

    public static boolean processModernCustomName(Item item, Supplier<CompoundTag> tag, Context context) {
        Tag nameTag = item.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_NAME);
        if (nameTag == null) return false;
        Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(nameTag);
        if (!tokens.isEmpty()) {
            item.setSparrowTagComponent(DataComponentKeys.CUSTOM_NAME, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(nameTag), tokens, context)));
            tag.get().put(DataComponentIds.CUSTOM_NAME, NetworkItemHandler.pack(Operation.ADD, nameTag));
            return true;
        }
        return false;
    }

    public static boolean processModernLore(Item item, Supplier<CompoundTag> tagSupplier, Context context) {
        Tag loreTag = item.getComponentAsSparrowTag(DataComponentTypes.LORE);
        boolean changed = false;
        if (!(loreTag instanceof ListTag listTag)) {
            return false;
        }
        ListTag newLore = new ListTag();
        for (Tag tag : listTag) {
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(tag);
            if (tokens.isEmpty()) {
                newLore.add(tag);
            } else {
                newLore.add(AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(tag), tokens, context)));
                changed = true;
            }
        }
        if (changed) {
            item.setSparrowTagComponent(DataComponentKeys.LORE, newLore);
            tagSupplier.get().put(DataComponentIds.LORE, NetworkItemHandler.pack(Operation.ADD, listTag));
            return true;
        }
        return false;
    }

    static class OtherItem {
        private final Item item;
        private final boolean forceReturn;
        private boolean globalChanged = false;
        private CompoundTag tag;

        public OtherItem(Item item, boolean forceReturn) {
            this.item = item;
            this.forceReturn = forceReturn;
        }

        public Optional<Item> process(Context context) {
            if (VersionHelper.isOrAbove1_21_5) {
                if (processModernLore(this.item, this::getOrCreateTag, context))
                    this.globalChanged = true;
                if (processModernCustomName(this.item, this::getOrCreateTag, context))
                    this.globalChanged = true;
                if (processModernItemName(this.item, this::getOrCreateTag, context))
                    this.globalChanged = true;
            } else {
                if (processLegacyLore(this.item, this::getOrCreateTag, context))
                    this.globalChanged = true;
                if (processLegacyCustomName(this.item, this::getOrCreateTag, context))
                    this.globalChanged = true;
                if (processLegacyItemName(this.item, this::getOrCreateTag, context))
                    this.globalChanged = true;
            }
            if (this.globalChanged) {
                CompoundTag customData = Optional.ofNullable(this.item.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_DATA))
                        .map(CompoundTag.class::cast)
                        .orElseGet(CompoundTag::new);
                customData.put(NETWORK_ITEM_TAG, ItemCrypto.encrypt(getOrCreateTag()));
                this.item.setSparrowTagComponent(DataComponentKeys.CUSTOM_DATA, customData);
                return Optional.of(this.item);
            } else if (this.forceReturn) {
                return Optional.of(this.item);
            } else {
                return Optional.empty();
            }
        }

        private CompoundTag getOrCreateTag() {
            if (this.tag == null) {
                this.tag = new CompoundTag();
            }
            return this.tag;
        }
    }
}
