package net.momirealms.craftengine.core.item.updater.impl;

import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaterType;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.List;
import java.util.Map;

public class ResetOperation<I> implements ItemUpdater<I> {
    public static final Type<?> TYPE = new Type<>();
    private final LazyReference<CustomItem<I>> item;
    private final List<Key> componentsToKeep;
    private final List<String[]> tagsToKeep;

    public ResetOperation(LazyReference<CustomItem<I>> item, List<Key> componentsToKeep, List<String[]> tagsToKeep) {
        this.componentsToKeep = componentsToKeep;
        this.tagsToKeep = tagsToKeep;
        this.item = item;
    }

    @Override
    public Item<I> update(Item<I> item, ItemBuildContext context) {
        Item<I> newItem = this.item.get().buildItem(context);
        if (VersionHelper.COMPONENT_RELEASE) {
            for (Key component : this.componentsToKeep) {
                if (item.hasComponent(component)) {
                    newItem.setExactComponent(component, item.getExactComponent(component));
                }
            }
        } else {
            for (String[] nbt : this.tagsToKeep) {
                if (item.hasTag((Object[]) nbt)) {
                    newItem.setTag(item.getTag((Object[]) nbt), (Object[]) nbt);
                }
            }
        }
        return newItem;
    }

    public static class Type<I> implements ItemUpdaterType<I> {

        @Override
        public ItemUpdater<I> create(Key item, Map<String, Object> args) {
            return new ResetOperation<>(
                    LazyReference.lazyReference(() -> {
                        ItemManager<I> itemManager = CraftEngine.instance().itemManager();
                        return itemManager.getCustomItem(item).orElseThrow();
                    }),
                    MiscUtils.getAsStringList(args.get("keep-components")).stream().map(Key::of).toList(),
                    MiscUtils.getAsStringList(args.get("keep-tags")).stream().map(tag -> tag.split("\\.")).toList()
            );
        }
    }
}
