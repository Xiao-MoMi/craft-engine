package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootConditions;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.*;
import java.util.function.Consumer;

public class SingleItemLootEntryContainer<T> extends AbstractSingleLootEntryContainer<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key item;

    protected SingleItemLootEntryContainer(Key item, List<Condition<LootContext>> conditions, List<LootFunction<T>> lootFunctions, int weight, int quality) {
        super(conditions, lootFunctions, weight, quality);
        this.item = item;
    }

    @Override
    public Key type() {
        return LootEntryContainers.ITEM;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void createItem(Consumer<Item<T>> lootConsumer, LootContext context) {
        Item<T> tItem = (Item<T>) CraftEngine.instance().itemManager().createWrappedItem(this.item, context.getOptionalParameter(DirectContextParameters.PLAYER).orElse(null));
        if (tItem != null) {
            lootConsumer.accept(tItem);
        } else {
            CraftEngine.instance().logger().warn("Failed to create item: " + this.item + " as loots. Please check if this item exists.");
        }
    }

    public static class Factory<A> implements LootEntryContainerFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootEntryContainer<A> create(Map<String, Object> arguments) {
            String itemObj = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("item"), "warning.config.loot_table.entry.item.missing_item");
            Key item = Key.from(itemObj);
            int weight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("weight", 1), "weight");
            int quality = ResourceConfigUtils.getAsInt(arguments.getOrDefault("quality", 0), "quality");
            List<Condition<LootContext>> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            List<LootFunction<A>> functions = Optional.ofNullable(arguments.get("functions"))
                    .map(it -> (List<LootFunction<A>>) new ArrayList<LootFunction<A>>(LootFunctions.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Collections.emptyList());
            return new SingleItemLootEntryContainer<>(item, conditions, functions, weight, quality);
        }
    }
}
