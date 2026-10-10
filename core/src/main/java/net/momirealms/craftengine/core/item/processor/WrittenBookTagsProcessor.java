package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class WrittenBookTagsProcessor implements NetworkItemProcessor {
    public static final WrittenBookTagsProcessor INSTANCE = new WrittenBookTagsProcessor();
    public static final ItemProcessorFactory<WrittenBookTagsProcessor> FACTORY = value -> INSTANCE;

    @Override
    public void apply(ItemBuildContext context) {
        this.apply(context, null);
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        return this.apply(context, networkData);
    }

    private boolean apply(ItemBuildContext context, @Nullable NetworkItemData networkData) {
        Item item = context.item();
        boolean changed = false;
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
                            if (tokens.isEmpty()) {
                                continue;
                            }
                            if (!changed && networkData != null) {
                                networkData.saveComponent(DataComponentKeys.WRITTEN_BOOK_CONTENT);
                            }
                            pagesTag.set(i, AdventureHelper.componentToNbt(AdventureHelper.replaceText(AdventureHelper.nbtToComponent(raw), tokens, context)));
                            changed = true;
                        }
                    }
                    if (changed) {
                        item.setSparrowTagComponent(DataComponentKeys.WRITTEN_BOOK_CONTENT, writtenBookTag);
                    }
                }
            }
        } else {
            if (item.getSparrowTag("pages") instanceof ListTag pagesTag) {
                for (int i = 0; i < pagesTag.size(); i++) {
                    String raw = pagesTag.getString(i);
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(raw);
                    if (tokens.isEmpty()) {
                        continue;
                    }
                    if (!changed && networkData != null) {
                        networkData.saveTag("pages");
                    }
                    pagesTag.set(i, new StringTag(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(raw), tokens, context))));
                    changed = true;
                }
                if (changed) {
                    item.setSparrowTag(pagesTag, "pages");
                }
            }
        }
        return changed;
    }
}
