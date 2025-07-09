package net.momirealms.craftengine.bukkit.item;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.modifier.ArgumentModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("DuplicatedCode")
public final class ModernNetworkItemHandler implements NetworkItemHandler<ItemStack> {

    @Override
    public Optional<Item<ItemStack>> c2s(Item<ItemStack> wrapped) {
        Tag customData = wrapped.getSparrowNBTComponent(ComponentTypes.CUSTOM_DATA);
        if (!(customData instanceof CompoundTag compoundTag)) return Optional.empty();
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        boolean hasDifferentMaterial = false;
        if (optionalCustomItem.isPresent()) {
            BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
            if (customItem.item() != FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject())) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.item(), wrapped.count());
                hasDifferentMaterial = true;
            }
        }
        CompoundTag networkData = compoundTag.getCompound(NETWORK_ITEM_TAG);
        if (networkData == null) {
            if (hasDifferentMaterial) {
                return Optional.of(wrapped);
            }
            return Optional.empty();
        }
        compoundTag.remove(NETWORK_ITEM_TAG);
        for (Map.Entry<String, Tag> entry : networkData.entrySet()) {
            if (entry.getValue() instanceof CompoundTag tag) {
                NetworkItemHandler.apply(entry.getKey(), tag, wrapped);
            }
        }
        if (compoundTag.isEmpty()) wrapped.resetComponent(ComponentTypes.CUSTOM_DATA);
        else wrapped.setNBTComponent(ComponentTypes.CUSTOM_DATA, compoundTag);
        return Optional.of(wrapped);
    }

    @Override
    public Optional<Item<ItemStack>> s2c(Item<ItemStack> wrapped, Player player) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) {
            if (!Config.interceptItem()) return Optional.empty();
            return new OtherItem(wrapped, false).process();
        } else {
            BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
            Object serverItem = FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject());
            boolean hasDifferentMaterial = serverItem == customItem.item() && serverItem != customItem.clientItem();
            if (hasDifferentMaterial) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.clientItem(), wrapped.count());
            }
            if (!customItem.hasClientBoundDataModifier()) {
                if (!Config.interceptItem() && !hasDifferentMaterial) return Optional.empty();
                return new OtherItem(wrapped, hasDifferentMaterial).process();
            } else {
                CompoundTag customData = Optional.ofNullable(wrapped.getSparrowNBTComponent(ComponentTypes.CUSTOM_DATA)).map(CompoundTag.class::cast).orElse(new CompoundTag());
                CompoundTag arguments = customData.getCompound(ArgumentModifier.ARGUMENTS_TAG);
                ItemBuildContext context;
                if (arguments == null) {
                    context = ItemBuildContext.of(player);
                } else {
                    ContextHolder.Builder builder = ContextHolder.builder();
                    for (Map.Entry<String, Tag> entry : arguments.entrySet()) {
                        builder.withParameter(ContextKey.direct(entry.getKey()), entry.getValue().getAsString());
                    }
                    context = ItemBuildContext.of(player, builder);
                }
                CompoundTag tag = new CompoundTag();
                for (ItemDataModifier<ItemStack> modifier : customItem.clientBoundDataModifiers()) {
                    modifier.prepareNetworkItem(wrapped, context, tag);
                }
                for (ItemDataModifier<ItemStack> modifier : customItem.clientBoundDataModifiers()) {
                    modifier.apply(wrapped, context);
                }
                if (Config.interceptItem()) {
                    if (!tag.containsKey(ComponentIds.ITEM_NAME)) {
                        if (VersionHelper.isOrAbove1_21_5()) processModernItemName(wrapped, () -> tag);
                        else processLegacyItemName(wrapped, () -> tag);
                    }
                    if (!tag.containsKey(ComponentIds.CUSTOM_NAME)) {
                        if (VersionHelper.isOrAbove1_21_5()) processModernCustomName(wrapped, () -> tag);
                        else processLegacyCustomName(wrapped, () -> tag);
                    }
                    if (!tag.containsKey(ComponentIds.LORE)) {
                        if (VersionHelper.isOrAbove1_21_5()) processModernLore(wrapped, () -> tag);
                        else processLegacyLore(wrapped, () -> tag);
                    }
                }
                if (tag.isEmpty()) {
                    if (hasDifferentMaterial) return Optional.of(wrapped);
                    return Optional.empty();
                }
                customData.put(NETWORK_ITEM_TAG, tag);
                wrapped.setNBTComponent(ComponentTypes.CUSTOM_DATA, customData);
                return Optional.of(wrapped);
            }
        }
    }

    public static boolean processLegacyLore(Item<ItemStack> item, Supplier<CompoundTag> tag) {
        Optional<List<String>> optionalLore = item.loreJson();
        if (optionalLore.isPresent()) {
            boolean changed = false;
            List<String> lore = optionalLore.get();
            List<String> newLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(line);
                if (tokens.isEmpty()) {
                    newLore.add(line);
                } else {
                    newLore.add(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens)));
                    changed = true;
                }
            }
            if (changed) {
                item.loreJson(newLore);
                ListTag listTag = new ListTag();
                for (String line : lore) {
                    listTag.add(new StringTag(line));
                }
                tag.get().put(ComponentIds.LORE, NetworkItemHandler.pack(Operation.ADD, listTag));
                return true;
            }
        }
        return false;
    }
    
    public static boolean processLegacyCustomName(Item<ItemStack> item, Supplier<CompoundTag> tag) {
        Optional<String> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            String line = optionalCustomName.get();
            Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(line);
            if (!tokens.isEmpty()) {
                item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens)));
                tag.get().put(ComponentIds.CUSTOM_NAME, NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    public static boolean processLegacyItemName(Item<ItemStack> item, Supplier<CompoundTag> tag) {
        Optional<String> optionalItemName = item.itemNameJson();
        if (optionalItemName.isPresent()) {
            String line = optionalItemName.get();
            Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(line);
            if (!tokens.isEmpty()) {
                item.itemNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens)));
                tag.get().put(ComponentIds.ITEM_NAME, NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    public static boolean processModernItemName(Item<ItemStack> item, Supplier<CompoundTag> tag) {
        Tag nameTag = item.getSparrowNBTComponent(ComponentTypes.ITEM_NAME);
        if (nameTag == null) return false;
        String tagStr = nameTag.getAsString();
        Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(tagStr);
        if (!tokens.isEmpty()) {
            item.setNBTComponent(ComponentKeys.ITEM_NAME, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(nameTag), tokens)));
            tag.get().put(ComponentIds.ITEM_NAME, NetworkItemHandler.pack(Operation.ADD, nameTag));
            return true;
        }
        return false;
    }

    public static boolean processModernCustomName(Item<ItemStack> item, Supplier<CompoundTag> tag) {
        Tag nameTag = item.getSparrowNBTComponent(ComponentTypes.CUSTOM_NAME);
        if (nameTag == null) return false;
        String tagStr = nameTag.getAsString();
        Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(tagStr);
        if (!tokens.isEmpty()) {
            item.setNBTComponent(ComponentKeys.CUSTOM_NAME, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(nameTag), tokens)));
            tag.get().put(ComponentIds.CUSTOM_NAME, NetworkItemHandler.pack(Operation.ADD, nameTag));
            return true;
        }
        return false;
    }

    public static boolean processModernLore(Item<ItemStack> item, Supplier<CompoundTag> tagSupplier) {
        Tag loreTag = item.getSparrowNBTComponent(ComponentTypes.LORE);
        boolean changed = false;
        if (!(loreTag instanceof ListTag listTag)) {
            return false;
        }
        ListTag newLore = new ListTag();
        for (Tag tag : listTag) {
            String tagStr = tag.getAsString();
            Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(tagStr);
            if (tokens.isEmpty()) {
                newLore.add(tag);
            } else {
                newLore.add(AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(tag), tokens)));
                changed = true;
            }
        }
        if (changed) {
            item.setNBTComponent(ComponentKeys.LORE, newLore);
            tagSupplier.get().put(ComponentIds.LORE, NetworkItemHandler.pack(Operation.ADD, listTag));
            return true;
        }
        return false;
    }

    static class OtherItem {
        private final Item<ItemStack> item;
        private final boolean forceReturn;
        private boolean globalChanged = false;
        private CompoundTag tag;

        public OtherItem(Item<ItemStack> item, boolean forceReturn) {
            this.item = item;
            this.forceReturn = forceReturn;
        }

        public Optional<Item<ItemStack>> process() {
            if (VersionHelper.isOrAbove1_21_5()) {
                if (processModernLore(this.item, this::getOrCreateTag))
                    this.globalChanged = true;
                if (processModernCustomName(this.item, this::getOrCreateTag))
                    this.globalChanged = true;
                if (processModernItemName(this.item, this::getOrCreateTag))
                    this.globalChanged = true;
            } else {
                if (processLegacyLore(this.item, this::getOrCreateTag))
                    this.globalChanged = true;
                if (processLegacyCustomName(this.item, this::getOrCreateTag))
                    this.globalChanged = true;
                if (processLegacyItemName(this.item, this::getOrCreateTag))
                    this.globalChanged = true;
            }
            if (this.globalChanged) {
                CompoundTag customData = Optional.ofNullable(this.item.getSparrowNBTComponent(ComponentTypes.CUSTOM_DATA)).map(CompoundTag.class::cast).orElse(new CompoundTag());
                customData.put(NETWORK_ITEM_TAG, getOrCreateTag());
                this.item.setNBTComponent(ComponentKeys.CUSTOM_DATA, customData);
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
