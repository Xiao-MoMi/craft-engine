package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.List;

public sealed interface ItemSelector permits ItemSelector.Item, ItemSelector.Tag {

    static ItemSelector.Item item(final Key key) {
        return new Item(key);
    }

    static ItemSelector.Tag tag(final Key key) {
        return new Tag(key);
    }

    record Item(Key id) implements ItemSelector {
    }

    record Tag(Key tag) implements ItemSelector {

        public List<UniqueKey> items() {
            return CraftEngine.instance().itemManager().itemIdsByTag(this.tag);
        }
    }
}
