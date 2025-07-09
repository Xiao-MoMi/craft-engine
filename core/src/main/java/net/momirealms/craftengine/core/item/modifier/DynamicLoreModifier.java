package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamicLoreModifier<I> implements ItemDataModifier<I> {
    public static final String CONTEXT_TAG_KEY = "craftengine:display_context";
    private final Map<String, List<String>> displayContexts;
    private final String defaultContext;

    public DynamicLoreModifier(Map<String, List<String>> displayContexts) {
        this.defaultContext = displayContexts.keySet().iterator().next();
        this.displayContexts = displayContexts;
    }

    public Map<String, List<String>> displayContexts() {
        return Collections.unmodifiableMap(this.displayContexts);
    }

    @Override
    public String name() {
        return "dynamic-lore";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        String displayContext = Optional.ofNullable(item.getJavaTag(CONTEXT_TAG_KEY)).orElse(this.defaultContext).toString();
        List<String> lore = this.displayContexts.get(displayContext);
        if (lore == null) {
            lore = this.displayContexts.get(this.defaultContext);
        }
        item.loreComponent(lore.stream().map(it -> AdventureHelper.miniMessage().deserialize(it, context.tagResolvers())).toList());
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.LORE);
            if (previous != null) {
                networkData.put(ComponentKeys.LORE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.LORE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getNBTTag("display", "Lore");
            if (previous != null) {
                networkData.put("display.Lore", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("display.Lore", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
