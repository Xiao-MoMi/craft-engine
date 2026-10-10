package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.ItemModelMappings;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.ItemLoreProxy;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModernCommonGlobalItemProcessor implements NetworkItemProcessor {

    @Override
    public boolean shouldSkip(ItemPacketSource source) {
        return !Config.interceptItem() && !(VersionHelper.isOrAbove1_21_2 && Config.obfuscateItemModel());
    }

    @Override
    public void apply(ItemBuildContext context) {
        this.applyNetworkItem((NetworkItemBuildContext) context, null);
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        Item item = context.item();
        ItemPacketSource source = context.source();
        boolean changed = false;
        if (Config.interceptItem()) {
            if (source.canSkipLore) {
                if (Config.minimizeItems() && item.hasComponent(DataComponentTypes.LORE)) {
                    if (networkData != null) {
                        networkData.saveComponentPatch(DataComponentKeys.LORE);
                    }
                    item.resetComponent(DataComponentTypes.LORE);
                    changed = true;
                }
            } else if (VersionHelper.isOrAbove1_21_5) {
                changed |= this.processModernLore(item, context, networkData);
            } else {
                changed |= this.processLegacyLore(item, context, networkData);
            }
            if (source.canSkipName) {
                if (Config.minimizeItems()) {
                    if (item.hasNonDefaultComponent(DataComponentTypes.ITEM_NAME)) {
                        if (networkData != null) {
                            networkData.saveComponentPatch(DataComponentKeys.ITEM_NAME);
                        }
                        item.resetComponent(DataComponentTypes.ITEM_NAME);
                        changed = true;
                    }
                    if (item.hasNonDefaultComponent(DataComponentTypes.CUSTOM_NAME)) {
                        if (networkData != null) {
                            networkData.saveComponentPatch(DataComponentKeys.CUSTOM_NAME);
                        }
                        item.resetComponent(DataComponentTypes.CUSTOM_NAME);
                        changed = true;
                    }
                }
            } else if (VersionHelper.isOrAbove1_21_5) {
                changed |= this.processModernCustomName(item, context, networkData);
                changed |= this.processModernItemName(item, context, networkData);
            } else {
                changed |= this.processLegacyCustomName(item, context, networkData);
                changed |= this.processLegacyItemName(item, context, networkData);
            }
        }
        if (VersionHelper.isOrAbove1_21_2 && Config.obfuscateItemModel()) {
            changed |= this.processItemModel(item, networkData);
        }
        return changed;
    }

    private boolean processItemModel(Item item, @Nullable NetworkItemData networkData) {
        Optional<String> itemModel = item.itemModel();
        if (itemModel.isEmpty()) {
            return false;
        }
        Key original = Key.of(itemModel.get());
        Key mapped = ItemModelMappings.getMappings().get(original);
        if (mapped == null || mapped.equals(original)) {
            return false;
        }

        if (networkData != null) {
            networkData.saveComponentPatch(DataComponentKeys.ITEM_MODEL);
        }
        item.itemModel(mapped.asString());
        return true;
    }

    private boolean processLegacyLore(Item item, Context context, @Nullable NetworkItemData networkData) {
        Optional<JsonArray> optionalLore = item.loreJson();
        if (optionalLore.isPresent()) {
            boolean changed = false;
            JsonArray lore = optionalLore.get();
            JsonArray newLore = new JsonArray();
            for (JsonElement element : lore) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(element);
                if (tokens.isEmpty()) {
                    newLore.add(element);
                } else {
                    newLore.add(AdventureHelper.componentToJsonElement(AdventureHelper.replaceText(AdventureHelper.jsonElementToComponent(element), tokens, context)));
                    changed = true;
                }
            }
            if (changed) {
                if (networkData != null) {
                    networkData.saveComponentPatch(DataComponentKeys.LORE);
                }
                item.loreJson(newLore);
                return true;
            }
        }
        return false;
    }

    private boolean processLegacyCustomName(Item item, Context context, @Nullable NetworkItemData networkData) {
        Optional<JsonElement> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            JsonElement json = optionalCustomName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(json);
            if (!tokens.isEmpty()) {
                if (networkData != null) {
                    networkData.saveComponentPatch(DataComponentKeys.CUSTOM_NAME);
                }
                item.customNameJson(AdventureHelper.componentToJsonElement(AdventureHelper.replaceText(AdventureHelper.jsonElementToComponent(json), tokens, context)));
                return true;
            }
        }
        return false;
    }

    private boolean processLegacyItemName(Item item, Context context, @Nullable NetworkItemData networkData) {
        Optional<JsonElement> optionalItemName = item.itemNameJson();
        if (optionalItemName.isPresent()) {
            JsonElement json = optionalItemName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(json);
            if (!tokens.isEmpty()) {
                if (networkData != null) {
                    networkData.saveComponentPatch(DataComponentKeys.ITEM_NAME);
                }
                item.itemNameJson(AdventureHelper.componentToJsonElement(AdventureHelper.replaceText(AdventureHelper.jsonElementToComponent(json), tokens, context)));
                return true;
            }
        }
        return false;
    }

    private boolean processModernItemName(Item item, Context context, @Nullable NetworkItemData networkData) {
        Object itemName = item.getExactComponent(DataComponentTypes.ITEM_NAME);
        if (itemName == null) {
            return false;
        }
        if (!ComponentUtils.hasNetworkTag(itemName)) {
            return false;
        }
        Tag nameTag = item.getComponentAsSparrowTag(DataComponentTypes.ITEM_NAME);
        if (nameTag == null) {
            return false;
        }
        Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(nameTag);
        if (!tokens.isEmpty()) {
            if (networkData != null) {
                networkData.saveComponentPatch(DataComponentKeys.ITEM_NAME);
            }
            item.setSparrowTagComponent(DataComponentKeys.ITEM_NAME, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(nameTag), tokens, context)));
            return true;
        }
        return false;
    }

    private boolean processModernCustomName(Item item, Context context, @Nullable NetworkItemData networkData) {
        Object customName = item.getExactComponent(DataComponentTypes.CUSTOM_NAME);
        if (customName == null) {
            return false;
        }
        if (!ComponentUtils.hasNetworkTag(customName)) {
            return false;
        }
        Tag nameTag = item.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_NAME);
        if (nameTag == null) {
            return false;
        }
        Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(nameTag);
        if (!tokens.isEmpty()) {
            if (networkData != null) {
                networkData.saveComponentPatch(DataComponentKeys.CUSTOM_NAME);
            }
            item.setSparrowTagComponent(DataComponentKeys.CUSTOM_NAME, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(nameTag), tokens, context)));
            return true;
        }
        return false;
    }

    private boolean processModernLore(Item item, Context context, @Nullable NetworkItemData networkData) {
        Object itemLore = item.getExactComponent(DataComponentTypes.LORE);
        if (itemLore == null) {
            return false;
        }
        List<Object> lines = ItemLoreProxy.INSTANCE.getStyleLines(itemLore);
        if (lines == null) {
            lines = ItemLoreProxy.INSTANCE.getLines(itemLore);
        }
        if (lines == null || lines.isEmpty()) {
            return false;
        }

        boolean has = false;
        for (Object line : lines) {
            if (ComponentUtils.hasNetworkTag(line)) {
                has = true;
                break;
            }
        }
        if (!has) {
            return false;
        }

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
            if (networkData != null) {
                networkData.saveComponentPatch(DataComponentKeys.LORE);
            }
            item.setSparrowTagComponent(DataComponentKeys.LORE, newLore);
            return true;
        }
        return false;
    }
}
