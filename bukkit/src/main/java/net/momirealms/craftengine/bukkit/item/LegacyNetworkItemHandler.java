package net.momirealms.craftengine.bukkit.item;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.item.modifier.ArgumentModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

@SuppressWarnings("DuplicatedCode")
public final class LegacyNetworkItemHandler implements NetworkItemHandler<ItemStack> {

    @Override
    public Optional<Item<ItemStack>> c2s(Item<ItemStack> wrapped) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        boolean hasDifferentMaterial = false;
        if (optionalCustomItem.isPresent()) {
            BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
            if (customItem.item() != FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject())) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.item(), wrapped.count());
                hasDifferentMaterial = true;
            }
        }
        if (!wrapped.hasTag(NETWORK_ITEM_TAG)) {
            if (hasDifferentMaterial) {
                return Optional.of(wrapped);
            }
        }
        CompoundTag networkData = (CompoundTag) wrapped.getNBTTag(NETWORK_ITEM_TAG);
        if (networkData == null) return Optional.empty();
        wrapped.removeTag(NETWORK_ITEM_TAG);
        for (Map.Entry<String, Tag> entry : networkData.entrySet()) {
            if (entry.getValue() instanceof CompoundTag tag) {
                NetworkItemHandler.apply(entry.getKey(), tag, wrapped);
            }
        }
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
                CompoundTag tag = new CompoundTag();
                Tag argumentTag = wrapped.getNBTTag(ArgumentModifier.ARGUMENTS_TAG);
                ItemBuildContext context;
                if (argumentTag instanceof CompoundTag arguments) {
                    ContextHolder.Builder builder = ContextHolder.builder();
                    for (Map.Entry<String, Tag> entry : arguments.entrySet()) {
                        builder.withParameter(ContextKey.direct(entry.getKey()), entry.getValue().getAsString());
                    }
                    context = ItemBuildContext.of(player, builder);
                } else {
                    context = ItemBuildContext.of(player);
                }
                for (ItemDataModifier<ItemStack> modifier : customItem.clientBoundDataModifiers()) {
                    modifier.prepareNetworkItem(wrapped, context, tag);
                }
                for (ItemDataModifier<ItemStack> modifier : customItem.clientBoundDataModifiers()) {
                    modifier.apply(wrapped, context);
                }
                if (Config.interceptItem()) {
                    if (!tag.containsKey("display.Name")) {
                        processCustomName(wrapped, tag::put);
                    }
                    if (!tag.containsKey("display.Lore")) {
                        processLore(wrapped, tag::put);
                    }
                }
                if (tag.isEmpty()) {
                    if (hasDifferentMaterial) {
                        return Optional.of(wrapped);
                    }
                    return Optional.empty();
                }
                wrapped.setTag(tag, NETWORK_ITEM_TAG);
                return Optional.of(wrapped);
            }
        }
    }

    public static boolean processCustomName(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback) {
        Optional<String> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            String line = optionalCustomName.get();
            Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(line);
            if (!tokens.isEmpty()) {
                item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens)));
                callback.accept("display.Name", NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    private static boolean processLore(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback) {
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

        public Optional<Item<ItemStack>> process() {
            if (processLore(this.item, (s, c) -> networkTag().put(s, c))) {
                this.globalChanged = true;
            }
            if (processCustomName(this.item, (s, c) -> networkTag().put(s, c))) {
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
