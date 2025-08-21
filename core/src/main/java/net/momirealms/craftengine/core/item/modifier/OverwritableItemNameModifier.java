package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

public class OverwritableItemNameModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final ItemNameModifier<I> modifier;

    public OverwritableItemNameModifier(String argument) {
        this.modifier = new ItemNameModifier<>(argument);
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(ComponentKeys.ITEM_NAME)) {
                return item;
            }
        } else {
            if (item.hasTag("display", "Name")) {
                return item;
            }
        }
        return this.modifier.apply(item, context);
    }

    @Override
    public Key type() {
        return ItemDataModifiers.OVERWRITABLE_ITEM_NAME;
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.ITEM_NAME;
    }

    @Override
    public @Nullable Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"display", "Name"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Name";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            return new OverwritableItemNameModifier<>(arg.toString());
        }
    }
}
