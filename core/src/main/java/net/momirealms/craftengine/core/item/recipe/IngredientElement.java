package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.List;

public sealed interface IngredientElement permits IngredientElement.Item, IngredientElement.Tag {

    static IngredientElement.Item item(final Key key) {
        return new Item(key);
    }
    static IngredientElement.Tag tag(final Key key) {
        return new Tag(key);
    }

    record Item(Key id) implements IngredientElement {
    }

    record Tag(Key tag) implements IngredientElement {

        public List<UniqueKey> items() {
            return CraftEngine.instance().itemManager().itemIdsByTag(this.tag);
        }
    }
}
