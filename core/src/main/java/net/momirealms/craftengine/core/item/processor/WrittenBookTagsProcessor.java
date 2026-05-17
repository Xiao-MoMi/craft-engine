package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;

public final class WrittenBookTagsProcessor implements ItemProcessor {
    public static final WrittenBookTagsProcessor INSTANCE = new WrittenBookTagsProcessor();
    public static final ItemProcessorFactory<WrittenBookTagsProcessor> FACTORY = value -> INSTANCE;
    private static final ContextKey<Boolean> HAS_NETWORK_TAG = ContextKey.direct("has_network_tag");

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (!context.getOptionalParameter(HAS_NETWORK_TAG).orElse(false)) return item;
        if (VersionHelper.COMPONENT_RELEASE) {
            CompoundTag writtenBookTag = (CompoundTag) item.getComponentAsSparrowTag(DataComponentKeys.WRITTEN_BOOK_CONTENT);
            if (writtenBookTag != null) {
                ListTag pagesTag = (ListTag) writtenBookTag.get("pages");
                if (pagesTag != null) {
                    for (int i = 0; i < pagesTag.size(); i++) {
                        CompoundTag pageTag = pagesTag.getCompound(i);
                        Tag raw = pageTag.get("raw");
                        if (raw != null) {
                            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(raw);
                            if (tokens.isEmpty()) continue;
                            pagesTag.set(i, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(raw), tokens, context)));
                        }
                    }
                    item.setSparrowTagComponent(DataComponentKeys.WRITTEN_BOOK_CONTENT, writtenBookTag);
                }
            }
        } else {
            if (item.getSparrowTag("pages") instanceof ListTag pagesTag) {
                for (int i = 0; i < pagesTag.size(); i++) {
                    String raw = pagesTag.getString(i);
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(raw);
                    if (tokens.isEmpty()) continue;
                    pagesTag.set(i, new StringTag(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(raw), tokens, context))));
                }
                item.setSparrowTag(pagesTag, "pages");
            }
        }
        return item;
    }

    @Override
    public Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.COMPONENT_RELEASE) {
            CompoundTag writtenBookTag = (CompoundTag) item.getComponentAsSparrowTag(DataComponentKeys.WRITTEN_BOOK_CONTENT);
            if (writtenBookTag != null) {
                ListTag pagesTag = (ListTag) writtenBookTag.get("pages");
                if (pagesTag != null) {
                    for (Tag rawPageTag : pagesTag) {
                        CompoundTag pageTag = (CompoundTag) rawPageTag;
                        Tag raw = pageTag.get("raw");
                        if (raw != null) {
                            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(raw);
                            if (!tokens.isEmpty()) {
                                context.contexts().withParameter(HAS_NETWORK_TAG, true);
                                networkData.put(DataComponentKeys.WRITTEN_BOOK_CONTENT.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, writtenBookTag));
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            if (item.getSparrowTag("pages") instanceof ListTag pagesTag) {
                for (Tag raw : pagesTag) {
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(raw);
                    if (!tokens.isEmpty()) {
                        context.contexts().withParameter(HAS_NETWORK_TAG, true);
                        networkData.put("pages", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, pagesTag));
                        break;
                    }
                }
            }
        }
        return item;
    }
}
