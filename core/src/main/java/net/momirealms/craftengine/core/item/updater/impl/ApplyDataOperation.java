package net.momirealms.craftengine.core.item.updater.impl;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaterType;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApplyDataOperation<I> implements ItemUpdater<I> {
    public static final Type<?> TYPE = new Type<>();
    private final List<ItemDataModifier<I>> modifiers;

    public ApplyDataOperation(List<ItemDataModifier<I>> modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public Item<I> update(Item<I> item, ItemBuildContext context) {
        if (this.modifiers != null) {
            for (ItemDataModifier<I> modifier : this.modifiers) {
                modifier.apply(item, context);
            }
        }
        return item;
    }

    public static class Type<I> implements ItemUpdaterType<I> {

        @SuppressWarnings("unchecked")
        @Override
        public ItemUpdater<I> create(Key item, Map<String, Object> args) {
            List<ItemDataModifier<I>> modifiers = new ArrayList<>();
            Map<String, Object> data = ResourceConfigUtils.getAsMap(args.get("data"), "data");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Optional.ofNullable(BuiltInRegistries.ITEM_DATA_MODIFIER_FACTORY.getValue(Key.withDefaultNamespace(entry.getKey(), Key.DEFAULT_NAMESPACE)))
                        .ifPresent(factory -> modifiers.add((ItemDataModifier<I>) factory.create(entry.getValue())));
            }
            return new ApplyDataOperation<>(modifiers);
        }
    }
}
