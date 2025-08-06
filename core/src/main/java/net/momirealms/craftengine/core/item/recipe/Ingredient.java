package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.*;
import java.util.function.Predicate;

public class Ingredient<T> implements Predicate<UniqueIdItem<T>>, StackedContents.IngredientInfo<UniqueKey> {
    // 自定义物品与原版物品混合的列表
    private final List<UniqueKey> items;
    // 自定义物品原版材质与原版物品混合的列表
    private final List<UniqueKey> vanillaItems;
    // ingredient里是否含有自定义物品
    private final boolean hasCustomItem;

    private Ingredient(List<UniqueKey> items, List<UniqueKey> vanillaItems, boolean hasCustomItem) {
        this.items = List.copyOf(items);
        this.vanillaItems = List.copyOf(vanillaItems);
        this.hasCustomItem = hasCustomItem;
    }

    public static <T> boolean isInstance(Optional<Ingredient<T>> optionalIngredient, UniqueIdItem<T> stack) {
        return optionalIngredient.map((ingredient) -> ingredient.test(stack))
                .orElseGet(stack::isEmpty);
    }

    public static <T> Ingredient<T> of(Set<UniqueKey> items, Set<UniqueKey> minecraftItems, boolean hasCustomItem) {
        return new Ingredient<>(new ArrayList<>(items), new ArrayList<>(minecraftItems), hasCustomItem);
    }

    public boolean hasCustomItem() {
        return hasCustomItem;
    }

    @Override
    public boolean test(UniqueIdItem<T> uniqueIdItem) {
        for (UniqueKey item : this.items()) {
            if (uniqueIdItem.is(item)) {
                return true;
            }
        }
        return false;
    }

    public List<UniqueKey> items() {
        return this.items;
    }

    public List<UniqueKey> minecraftItems() {
        return vanillaItems;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (UniqueKey item : this.items()) {
            joiner.add(item.toString());
        }
        return "Ingredient: [" + joiner + "]";
    }

    public boolean isEmpty() {
        return this.items().isEmpty();
    }

    @Override
    public boolean acceptsItem(UniqueKey entry) {
        return this.items.contains(entry);
    }
}


