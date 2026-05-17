package net.momirealms.craftengine.core.item.updater.impl;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaterFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.List;

// todo 完善 custom data 保留机制
public final class ResetOperation implements ItemUpdater {
    public static final ItemUpdaterFactory<ResetOperation> FACTORY = new Factory();
    private final LazyReference<ItemDefinition> item;
    private final List<Key> componentsToKeep;
    private final List<String[]> tagsToKeep;

    public ResetOperation(LazyReference<ItemDefinition> item, List<Key> componentsToKeep, List<String[]> tagsToKeep) {
        this.componentsToKeep = componentsToKeep;
        this.tagsToKeep = tagsToKeep;
        this.item = item;
    }

    @Override
    public Item update(Item item, ItemBuildContext context) {
        Item newItem = this.item.get().buildItem(context);
        if (VersionHelper.COMPONENT_RELEASE) {
            for (Key component : this.componentsToKeep) {
                if (item.hasComponent(component)) {
                    newItem.setExactComponent(component, item.getExactComponent(component));
                }
            }
        } else {
            for (String[] nbt : this.tagsToKeep) {
                if (item.hasTag((Object[]) nbt)) {
                    newItem.setTag(item.getSparrowTag((Object[]) nbt), (Object[]) nbt);
                }
            }
        }
        return newItem;
    }

    private static class Factory implements ItemUpdaterFactory<ResetOperation> {
        private static final String[] KEEP_COMPONENTS = new String[] {"keep_components", "keep-components"};
        private static final String[] KEEP_TAGS = new String[] {"keep_tags", "keep-tags"};

        @Override
        public ResetOperation create(Key item, ConfigSection section) {
            return new ResetOperation(
                    LazyReference.lazyReference(() -> CraftEngine.instance().itemManager().getItemDefinition(item).orElseThrow()),
                    section.getList(KEEP_COMPONENTS, ConfigValue::getAsIdentifier),
                    section.getList(KEEP_TAGS, v -> v.getAsString().split("\\."))
            );
        }
    }
}
