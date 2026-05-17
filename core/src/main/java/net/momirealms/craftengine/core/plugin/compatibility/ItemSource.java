package net.momirealms.craftengine.core.plugin.compatibility;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.jetbrains.annotations.Nullable;

public interface ItemSource {

    String plugin();

    @Nullable
    Item build(String id, ItemBuildContext context);

    String id(Item item);
}