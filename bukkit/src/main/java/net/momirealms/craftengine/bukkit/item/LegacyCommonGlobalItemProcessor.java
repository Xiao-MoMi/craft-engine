package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public final class LegacyCommonGlobalItemProcessor implements NetworkItemProcessor {
    private static final Object[] DISPLAY_NAME = {"display", "Name"};
    private static final Object[] DISPLAY_LORE = {"display", "Lore"};

    @Override
    public boolean shouldSkip(ItemPacketSource source) {
        return !Config.interceptItem();
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
        if (source.canSkipLore) {
            if (Config.minimizeItems() && item.hasTag(DISPLAY_LORE)) {
                if (networkData != null) {
                    networkData.saveTag("display.Lore", DISPLAY_LORE);
                }
                item.removeTag(DISPLAY_LORE);
                changed = true;
            }
        } else {
            changed |= this.processLore(item, context, networkData);
        }
        if (source.canSkipName) {
            if (Config.minimizeItems() && item.hasTag(DISPLAY_NAME)) {
                if (networkData != null) {
                    networkData.saveTag("display.Name", DISPLAY_NAME);
                }
                item.removeTag(DISPLAY_NAME);
                changed = true;
            }
        } else {
            changed |= this.processCustomName(item, context, networkData);
        }
        return changed;
    }

    private boolean processCustomName(Item item, Context context, @Nullable NetworkItemData networkData) {
        Optional<JsonElement> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            JsonElement json = optionalCustomName.get();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(json);
            if (!tokens.isEmpty()) {
                if (networkData != null) {
                    networkData.saveTag("display.Name", DISPLAY_NAME);
                }
                item.customNameJson(AdventureHelper.componentToJsonElement(AdventureHelper.replaceText(AdventureHelper.jsonElementToComponent(json), tokens, context)));
                return true;
            }
        }
        return false;
    }

    private boolean processLore(Item item, Context context, @Nullable NetworkItemData networkData) {
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
                    networkData.saveTag("display.Lore", DISPLAY_LORE);
                }
                item.loreJson(newLore);
                return true;
            }
        }
        return false;
    }
}
