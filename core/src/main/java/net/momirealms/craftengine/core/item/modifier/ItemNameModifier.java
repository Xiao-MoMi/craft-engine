package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public class ItemNameModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final String argument;
    private final FormattedLine line;

    public ItemNameModifier(String argument) {
        this.argument = argument;
        this.line = FormattedLine.create(argument);
    }

    public String itemName() {
        return argument;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.ITEM_NAME;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.itemNameComponent(this.line.parse(context));
        return item;
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
            String name = arg.toString();
            return new ItemNameModifier<>(name);
        }
    }
}
