package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.item.network.NetworkItemPipeline;
import net.momirealms.craftengine.core.item.network.encrypt.ItemCrypto;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.proxy.minecraft.nbt.ByteTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.ListTagProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class LegacyNetworkItemHandler implements NetworkItemHandler {

    @Override
    public Optional<Item> c2s(Item wrapped) {
        boolean forceReturn = false;

        // 处理收纳袋
        Object bundleContents = wrapped.getMinecraftTag("Items");
        if (bundleContents != null) {
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            for (Object tag : (Iterable<?>) bundleContents) {
                Object previousItem = ItemStackProxy.INSTANCE.of(tag);
                Optional<ItemStack> itemStack = BukkitItemManager.instance().c2s(ItemStackUtils.getBukkitStack(previousItem));
                if (itemStack.isPresent()) {
                    newItems.add(ItemStackUtils.unwrap(itemStack.get()));
                    changed = true;
                } else {
                    newItems.add(previousItem);
                }
            }
            if (changed) {
                Object listTag = ListTagProxy.INSTANCE.newInstance();
                for (Object newItem : newItems) {
                    ListTagProxy.INSTANCE.addTag(listTag, 0, ItemStackProxy.INSTANCE.save(newItem, CompoundTagProxy.INSTANCE.newInstance()));
                }
                wrapped.setTag(listTag, "Items");
                forceReturn = true;
            }
        }

        // 处理container
        Object containerContents = wrapped.getMinecraftTag("BlockEntityTag");
        if (containerContents != null) {
            Object itemTags = CompoundTagProxy.INSTANCE.get(containerContents, "Items");
            if (itemTags != null) {
                boolean changed = false;
                List<Pair<Byte, Object>> newItems = new ArrayList<>();
                for (Object tag : (Iterable<?>) itemTags) {
                    Object previousItem = ItemStackProxy.INSTANCE.of(tag);
                    Optional<ItemStack> itemStack = BukkitItemManager.instance().c2s(ItemStackUtils.getBukkitStack(previousItem));
                    byte slot = ByteTagProxy.INSTANCE.value(CompoundTagProxy.INSTANCE.get(tag, "Slot"));
                    if (itemStack.isPresent()) {
                        newItems.add(Pair.of(slot, ItemStackUtils.unwrap(itemStack.get())));
                        changed = true;
                    } else {
                        newItems.add(Pair.of(slot, previousItem));
                    }
                }
                if (changed) {
                    Object listTag = ListTagProxy.INSTANCE.newInstance();
                    for (Pair<Byte, Object> newItem : newItems) {
                        Object newTag = ItemStackProxy.INSTANCE.save(newItem.right(), CompoundTagProxy.INSTANCE.newInstance());
                        Object slotTag = ByteTagProxy.INSTANCE.valueOf(newItem.left());
                        CompoundTagProxy.INSTANCE.put(newTag, "Slot", slotTag);
                        ListTagProxy.INSTANCE.addTag(listTag, 0, newTag);
                    }
                    wrapped.setTag(listTag, "BlockEntityTag", "Items");
                    forceReturn = true;
                }
            }
        }

        ItemDefinition customItemDefinition = wrapped.getDefinitionOrNull();
        if (customItemDefinition != null) {
            BukkitItemDefinition customItem = (BukkitItemDefinition) customItemDefinition;
            if (customItem.item() != ItemStackProxy.INSTANCE.getItem(wrapped.minecraftItem())) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.item(), wrapped.count());
                forceReturn = true;
            }
        }

        CompoundTag networkData = ItemCrypto.decrypt(wrapped.getSparrowTag(NETWORK_ITEM_TAG));
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
    public Optional<Item> s2c(Item wrapped, @Nullable Player player, ItemPacketSource source) {
        boolean forceReturn = false;

        // 处理收纳袋
        Object bundleContents = wrapped.getMinecraftTag("Items");
        if (bundleContents != null) {
            List<Object> newItems = new ArrayList<>();
            boolean changed = false;
            for (Object tag : (Iterable<?>) bundleContents) {
                Object previousItem = ItemStackProxy.INSTANCE.of(tag);
                Optional<ItemStack> itemStack = BukkitItemManager.instance().s2c(ItemStackUtils.getBukkitStack(previousItem), player, source);
                if (itemStack.isPresent()) {
                    newItems.add(ItemStackUtils.unwrap(itemStack.get()));
                    changed = true;
                } else {
                    newItems.add(previousItem);
                }
            }
            if (changed) {
                Object listTag = ListTagProxy.INSTANCE.newInstance();
                for (Object newItem : newItems) {
                    ListTagProxy.INSTANCE.addTag(listTag, 0, ItemStackProxy.INSTANCE.save(newItem, CompoundTagProxy.INSTANCE.newInstance()));
                }
                wrapped.setTag(listTag, "Items");
                forceReturn = true;
            }
        }

        // 处理container
        Object containerContents = wrapped.getMinecraftTag("BlockEntityTag");
        if (containerContents != null) {
            Object itemTags = CompoundTagProxy.INSTANCE.get(containerContents, "Items");
            if (itemTags != null) {
                boolean changed = false;
                List<Pair<Byte, Object>> newItems = new ArrayList<>();
                for (Object tag : (Iterable<?>) itemTags) {
                    Object previousItem = ItemStackProxy.INSTANCE.of(tag);
                    Optional<ItemStack> itemStack = BukkitItemManager.instance().s2c(ItemStackUtils.getBukkitStack(previousItem), player, source);
                    byte slot = ByteTagProxy.INSTANCE.value(CompoundTagProxy.INSTANCE.get(tag, "Slot"));
                    if (itemStack.isPresent()) {
                        newItems.add(Pair.of(slot, ItemStackUtils.unwrap(itemStack.get())));
                        changed = true;
                    } else {
                        newItems.add(Pair.of(slot, previousItem));
                    }
                }
                if (changed) {
                    Object listTag = ListTagProxy.INSTANCE.newInstance();
                    for (Pair<Byte, Object> newItem : newItems) {
                        Object newTag = ItemStackProxy.INSTANCE.save(newItem.right(), CompoundTagProxy.INSTANCE.newInstance());
                        Object slotTag = ByteTagProxy.INSTANCE.valueOf(newItem.left());
                        CompoundTagProxy.INSTANCE.put(newTag, "Slot", slotTag);
                        ListTagProxy.INSTANCE.addTag(listTag, 0, newTag);
                    }
                    wrapped.setTag(listTag, "BlockEntityTag", "Items");
                    forceReturn = true;
                }
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
        NetworkItemPipeline.Result result = NetworkItemPipeline.process(original, wrapped, player, source, itemProcessors, BukkitItemManager.instance().globalNetworkItemProcessors());
        wrapped = result.item();
        forceReturn |= result.changed();
        CompoundTag tag = result.networkData();
        if (tag != null && !tag.isEmpty()) {
            wrapped.setTag(ItemCrypto.encrypt(tag), NETWORK_ITEM_TAG);
            forceReturn = true;
        }
        return forceReturn ? Optional.of(wrapped) : Optional.empty();
    }

}
