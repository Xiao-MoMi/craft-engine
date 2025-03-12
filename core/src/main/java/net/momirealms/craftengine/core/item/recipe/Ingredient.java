package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

import java.util.*;
import java.util.function.Predicate;

public class Ingredient<T> implements Predicate<OptimizedIDItem<T>>, StackedContents.IngredientInfo<Holder<Key>> {
    private final List<Holder<Key>> items;

    public Ingredient(List<Holder<Key>> items) {
        this.items = items;
    }

    public static <T> boolean isInstance(Optional<Ingredient<T>> optionalIngredient, OptimizedIDItem<T> stack) {
        return optionalIngredient.map((ingredient) -> ingredient.test(stack))
                .orElseGet(stack::isEmpty);
    }

    public static <T> Ingredient<T> of(List<Holder<Key>> items) {
        return new Ingredient<>(items);
    }

    public static <T> Ingredient<T> of(Set<Holder<Key>> items) {
        return new Ingredient<>(new ArrayList<>(items));
    }

    @Override
    public boolean test(OptimizedIDItem<T> optimizedIDItem) {
        for (Holder<Key> item : this.items()) {
            if (optimizedIDItem.is(item)) {
                return true;
            }
        }
        return false;
    }

    public List<Holder<Key>> items() {
        return this.items;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (Holder<Key> item : this.items()) {
            joiner.add(item.toString());
        }
        return "Ingredient: [" + joiner + "]";
    }

    public boolean isEmpty() {
        return this.items().isEmpty();
    }

    @Override
    public boolean acceptsItem(Holder<Key> entry) {
        return this.items.contains(entry);
    }
}


