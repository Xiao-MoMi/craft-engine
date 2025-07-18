package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.*;
import java.util.function.Predicate;

public class Ingredient<T> implements Predicate<UniqueIdItem<T>>, StackedContents.IngredientInfo<UniqueKey> {
    private final List<UniqueKey> items;

    public Ingredient(List<UniqueKey> items) {
        this.items = items;
    }

    public static <T> boolean isInstance(Optional<Ingredient<T>> optionalIngredient, UniqueIdItem<T> stack) {
        return optionalIngredient.map((ingredient) -> ingredient.test(stack))
                .orElseGet(stack::isEmpty);
    }

    public static <T> Ingredient<T> of(List<UniqueKey> items) {
        return new Ingredient<>(items);
    }

    public static <T> Ingredient<T> of(Set<UniqueKey> items) {
        return new Ingredient<>(new ArrayList<>(items));
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


