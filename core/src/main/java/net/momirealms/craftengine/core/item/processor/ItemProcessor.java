package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.ItemBuildContext;

public interface ItemProcessor {

    void apply(ItemBuildContext context);

    default boolean isConstant() {
        return false;
    }
}
