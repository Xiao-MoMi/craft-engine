package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.item.modifier.ArgumentsModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
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
    public Item<ItemStack> c2s(Item<ItemStack> wrapped) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
            if (customItem.item() != FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject())) {
                wrapped = wrapped.unsafeTransmuteCopy(customItem.item(), wrapped.count());
            }
        }
        CompoundTag networkData = (CompoundTag) wrapped.getTag(NETWORK_ITEM_TAG);
        if (networkData == null) return wrapped;
        wrapped.removeTag(NETWORK_ITEM_TAG);
        for (Map.Entry<String, Tag> entry : networkData.entrySet()) {
            if (entry.getValue() instanceof CompoundTag tag) {
                NetworkItemHandler.apply(entry.getKey(), tag, wrapped);
            }
        }
        return wrapped;
    }

    @Override
    public Item<ItemStack> s2c(Item<ItemStack> wrapped, Player player) {
        // todo 处理bundle和container

        // todo 处理book

        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        // 不是自定义物品或修改过的原版物品
        if (optionalCustomItem.isEmpty()) {
            if (!Config.interceptItem()) return wrapped;
            return new OtherItem(wrapped).process(NetworkTextReplaceContext.of(player));
        }

        // 应用client-bound-material
        BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
        Object serverItem = FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject());
        boolean hasDifferentMaterial = serverItem == customItem.item() && serverItem != customItem.clientItem();
        if (hasDifferentMaterial) {
            wrapped = wrapped.unsafeTransmuteCopy(customItem.clientItem(), wrapped.count());
        }

        // 没有客户端侧组件
        if (!customItem.hasClientBoundDataModifier()) {
            if (!Config.interceptItem()) return wrapped;
            return new OtherItem(wrapped).process(NetworkTextReplaceContext.of(player));
        }

        // 应用client-bound-data
        CompoundTag tag = new CompoundTag();
        Tag argumentTag = wrapped.getTag(ArgumentsModifier.ARGUMENTS_TAG);
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
                processCustomName(wrapped, tag::put, context);
            }
            if (!tag.containsKey("display.Lore")) {
                processLore(wrapped, tag::put, context);
            }
        }
        if (!tag.isEmpty()) {
            wrapped.setTag(tag, NETWORK_ITEM_TAG);
        }
        return wrapped;
    }

    public static boolean processCustomName(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback, Context context) {
        Optional<String> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            String line = optionalCustomName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(line);
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
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(line);
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

        public OtherItem(Item<ItemStack> item) {
            this.item = item;
        }

        public Item<ItemStack> process(Context context) {
            if (processLore(this.item, (s, c) -> networkTag().put(s, c), context)) {
                this.globalChanged = true;
            }
            if (processCustomName(this.item, (s, c) -> networkTag().put(s, c), context)) {
                this.globalChanged = true;
            }
            if (this.globalChanged) {
                this.item.setTag(this.networkTag, NETWORK_ITEM_TAG);
            }
            return this.item;
        }

        public CompoundTag networkTag() {
            if (this.networkTag == null) {
                this.networkTag = new CompoundTag();
            }
            return this.networkTag;
        }
    }
}
