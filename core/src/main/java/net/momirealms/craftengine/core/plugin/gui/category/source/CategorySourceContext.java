package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class CategorySourceContext {
    private final Pack pack;
    private final List<Key> itemIds;
    private final Predicate<Key> vanillaItemPredicate;

    public CategorySourceContext(
            Pack pack,
            List<Key> itemIds,
            Predicate<Key> vanillaItemPredicate
    ) {
        this.pack = Objects.requireNonNull(pack, "pack");
        this.itemIds = List.copyOf(itemIds);
        this.vanillaItemPredicate = Objects.requireNonNull(vanillaItemPredicate, "vanillaItemPredicate");
    }

    public static CategorySourceContext of(Pack pack, ItemManager itemManager) {
        return new CategorySourceContext(
                pack,
                itemManager.allItemIds(),
                itemManager::isVanillaItem
        );
    }

    public Pack pack() {
        return this.pack;
    }

    public List<Key> itemIds() {
        return this.itemIds;
    }

    public boolean isVanillaItem(Key itemId) {
        return this.vanillaItemPredicate.test(itemId);
    }
}
