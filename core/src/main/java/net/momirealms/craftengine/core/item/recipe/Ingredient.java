package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemSelector;
import net.momirealms.craftengine.core.item.recipe.predicate.DataComponentPredicate;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

public final class Ingredient implements Predicate<UniqueIdItem>, StackedContents.IngredientInfo<UniqueIdItem> {
    public final List<ItemSelector> elements;
    // 自定义物品与原版物品混合的列表
    public final List<UniqueKey> items;
    // 自定义物品原版材质与原版物品混合的列表
    public final List<UniqueKey> vanillaItems;
    // ingredient里是否含有自定义物品
    public final boolean hasCustomItem;
    public final int count;
    @Nullable
    public final DataComponentPredicate predicate;

    private Ingredient(List<ItemSelector> elements,
                       List<UniqueKey> items,
                       List<UniqueKey> vanillaItems,
                       boolean hasCustomItem,
                       int count,
                       @Nullable DataComponentPredicate predicate) {
        this.elements = List.copyOf(elements);
        this.items = List.copyOf(items);
        this.vanillaItems = List.copyOf(vanillaItems);
        this.hasCustomItem = hasCustomItem;
        this.count = count;
        this.predicate = predicate;
    }

    public int count() {
        return this.count;
    }

    @Nullable
    public DataComponentPredicate predicate() {
        return this.predicate;
    }

    public Item applyPredicateLooks(Item item) {
        if (this.predicate != null) {
            this.predicate.apply(item);
        }
        return item;
    }

    public List<ItemSelector> elements() {
        return this.elements;
    }

    public boolean hasCustomItem() {
        return this.hasCustomItem;
    }

    public List<UniqueKey> items() {
        return this.items;
    }

    public List<UniqueKey> minecraftItems() {
        return vanillaItems;
    }

    public static boolean isInstance(Optional<Ingredient> optionalIngredient, UniqueIdItem stack) {
        return optionalIngredient.map((ingredient) -> ingredient.test(stack))
                .orElseGet(stack::isEmpty);
    }

    public static Ingredient of(List<ItemSelector> elements, Set<UniqueKey> items, Set<UniqueKey> minecraftItems, boolean hasCustomItem, int count, DataComponentPredicate predicate) {
        return new Ingredient(elements, List.copyOf(items), List.copyOf(minecraftItems), hasCustomItem, count, predicate);
    }

    public static Ingredient of(List<ItemSelector> elements, Set<UniqueKey> items, Set<UniqueKey> minecraftItems, boolean hasCustomItem) {
        return new Ingredient(elements, List.copyOf(items), List.copyOf(minecraftItems), hasCustomItem, 1, null);
    }

    @Override
    public boolean test(UniqueIdItem uniqueIdItem) {
        for (UniqueKey item : this.items()) {
            if (uniqueIdItem.is(item)) {
                if (this.predicate != null && !this.predicate.test(uniqueIdItem.item())) {
                    return false;
                }
                if (this.count == 1) {
                    return true;
                } else {
                    return uniqueIdItem.item().count() >= this.count;
                }
            }
        }
        return false;
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
    public boolean acceptsItem(UniqueIdItem entry) {
        if (!this.items.contains(entry.id())) {
            return false;
        }
        if (this.predicate != null && !this.predicate.test(entry.item())) {
            return false;
        }
        return this.count <= 1 || entry.item().count() >= this.count;
    }
}


