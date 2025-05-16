package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootConditions;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class FurnitureItemLootEntryContainer<T> extends SingleItemLootEntryContainer<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final boolean hasFallback;

    protected FurnitureItemLootEntryContainer(@Nullable Key item, List<Condition<LootContext>> conditions, List<LootFunction<T>> lootFunctions, int weight, int quality) {
        super(item, conditions, lootFunctions, weight, quality);
        this.hasFallback = item != null;
    }

    @Override
    public Key type() {
        return LootEntryContainers.FURNITURE_ITEM;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void createItem(Consumer<Item<T>> lootConsumer, LootContext context) {
        Optional<Item<?>> optionalItem = context.getOptionalParameter(DirectContextParameters.FURNITURE_ITEM);
        if (optionalItem.isPresent()) {
            lootConsumer.accept((Item<T>) optionalItem.get());
        } else if (this.hasFallback) {
            super.createItem(lootConsumer, context);
        }
    }

    public static class Factory<A> implements LootEntryContainerFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootEntryContainer<A> create(Map<String, Object> arguments) {
            Key item = Optional.ofNullable(arguments.get("item")).map(String::valueOf).map(Key::of).orElse(null);
            int weight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("weight", 1), "weight");
            int quality = ResourceConfigUtils.getAsInt(arguments.getOrDefault("quality", 0), "quality");
            List<Condition<LootContext>> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            List<LootFunction<A>> functions = Optional.ofNullable(arguments.get("functions"))
                    .map(it -> (List<LootFunction<A>>) new ArrayList<LootFunction<A>>(LootFunctions.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Collections.emptyList());
            return new FurnitureItemLootEntryContainer<>(item, conditions, functions, weight, quality);
        }
    }
}
