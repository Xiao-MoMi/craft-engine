package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.component.DataComponentIds;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.item.network.NetworkItemPipeline;
import net.momirealms.craftengine.core.item.network.encrypt.ItemCrypto;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.PatchedDataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.BundleContentsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.ItemContainerContentsProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
            List<Object> bundleItems = BundleContentsProxy.INSTANCE.getItems(bundleContents);
            List<Object> newItems = new ArrayList<>(bundleItems.size());
            boolean changed = false;
            if (VersionHelper.isOrAbove26_1) {
                for (Object itemTemplate : bundleItems) {
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
                for (Object previousItem : bundleItems) {
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
            List<Object> containerItems = ItemContainerContentsProxy.INSTANCE.getItems(containerContents);
            List<Object> newItems = new ArrayList<>(containerItems.size());
            boolean changed = false;
            if (VersionHelper.isOrAbove26_1) {
                for (Object previousItem : containerItems) {
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
                for (Object previousItem : containerItems) {
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

        ItemDefinition itemDefinition = wrapped.getDefinitionOrNull();
        // 一定要先尝试恢复client-bound-material，再应用组件变化
        if (itemDefinition != null) {
            BukkitItemDefinition customItem = (BukkitItemDefinition) itemDefinition;
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
    public Optional<Item> s2c(Item wrapped, @Nullable Player player, ItemPacketSource source) {
        boolean forceReturn = false;

        // 处理收纳袋
        if (wrapped.hasComponent(DataComponentTypes.BUNDLE_CONTENTS)) {
            Object bundleContents = wrapped.getExactComponent(DataComponentTypes.BUNDLE_CONTENTS);
            List<Object> bundleItems = BundleContentsProxy.INSTANCE.getItems(bundleContents);
            List<Object> newItems = new ArrayList<>(bundleItems.size());
            boolean changed = false;
            if (VersionHelper.isOrAbove26_1) {
                for (Object itemTemplate : bundleItems) {
                    Object previousItem = ItemStackTemplateProxy.INSTANCE.create(itemTemplate);
                    Optional<Item> converted = this.itemManager.s2c(this.itemManager.wrap(previousItem), player, source);
                    if (converted.isPresent()) {
                        newItems.add(ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(converted.get().minecraftItem()));
                        changed = true;
                    } else {
                        newItems.add(itemTemplate);
                    }
                }
            } else {
                for (Object previousItem : bundleItems) {
                    Optional<Item> converted = this.itemManager.s2c(this.itemManager.wrap(previousItem).copy(), player, source);
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
            List<Object> containerItems = ItemContainerContentsProxy.INSTANCE.getItems(containerContents);
            boolean changed = false;
            List<Object> newItems = new ArrayList<>(containerItems.size());
            if (VersionHelper.isOrAbove26_1) {
                for (Object optionalTemplate : containerItems) {
                    @SuppressWarnings("unchecked")
                    Optional<Object> previousTemplate = (Optional<Object>) optionalTemplate;
                    if (previousTemplate.isPresent()) {
                        Object itemTemplate = previousTemplate.get();
                        Object previousItem = ItemStackTemplateProxy.INSTANCE.create(itemTemplate);
                        Optional<Item> converted = this.itemManager.s2c(this.itemManager.wrap(previousItem), player, source);
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
                for (Object previousItem : containerItems) {
                    Optional<Item> itemStack = this.itemManager.s2c(this.itemManager.wrap(previousItem).copy(), player, source);
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

        ItemDefinition customItemDefinition = wrapped.getDefinitionOrNull();
        Item original = wrapped;
        NetworkItemProcessor[] itemProcessors = NetworkItemPipeline.EMPTY_PROCESSORS;
        if (customItemDefinition != null) {
            BukkitItemDefinition customItem = (BukkitItemDefinition) customItemDefinition;
            if (customItem.hasClientboundMaterial() && ItemStackProxy.INSTANCE.getItem(wrapped.minecraftItem()) != customItem.clientItem()) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.clientItem(), wrapped.count());
                forceReturn = true;
            }
            itemProcessors = customItem.clientBoundProcessors();
        }
        NetworkItemPipeline.Result result = NetworkItemPipeline.process(original, wrapped, player, source, itemProcessors, this.itemManager.globalNetworkItemProcessors());
        wrapped = result.item();
        forceReturn |= result.changed();
        CompoundTag tag = result.networkData();
        if (tag != null && !tag.isEmpty()) {
            CompoundTag customData = (CompoundTag) wrapped.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_DATA);
            if (customData == null) {
                customData = new CompoundTag();
            }
            customData.put(NETWORK_ITEM_TAG, ItemCrypto.encrypt(tag));
            wrapped.setSparrowTagComponent(DataComponentTypes.CUSTOM_DATA, customData);
            forceReturn = true;
        }
        if (customItemDefinition != null && customItemDefinition.hasClientBoundProcessor()) {
            Object clientItem = ItemStackProxy.INSTANCE.getItem(wrapped.minecraftItem());
            if (rebase(wrapped.minecraftItem(), this.itemManager.originalVanillaItemComponents(clientItem))) {
                forceReturn = true;
            }
        }
        return forceReturn ? Optional.of(wrapped) : Optional.empty();
    }

    static boolean rebase(Object itemStack, @Nullable Object originalPrototype) {
        if (originalPrototype == null) {
            return false;
        }

        Object effectiveComponents = ItemStackProxy.INSTANCE.getComponents(itemStack);
        Object rebasedComponents = PatchedDataComponentMapProxy.INSTANCE.newInstance(originalPrototype);
        PatchedDataComponentMapProxy.INSTANCE.setAll(rebasedComponents, effectiveComponents);

        Set<Object> effectiveTypes = DataComponentMapProxy.INSTANCE.keySet(effectiveComponents);
        for (Object type : DataComponentMapProxy.INSTANCE.keySet(originalPrototype)) {
            if (requiresExplicitRemoval(effectiveTypes, type)) {
                PatchedDataComponentMapProxy.INSTANCE.remove(rebasedComponents, type);
            }
        }

        ItemStackProxy.INSTANCE.setComponents(itemStack, rebasedComponents);
        return true;
    }

    static boolean requiresExplicitRemoval(Set<Object> effectiveTypes, Object originalType) {
        return !effectiveTypes.contains(originalType);
    }

}
