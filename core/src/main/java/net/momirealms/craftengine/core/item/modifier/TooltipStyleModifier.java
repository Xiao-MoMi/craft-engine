package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public class TooltipStyleModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key argument;

    public TooltipStyleModifier(Key argument) {
        this.argument = argument;
    }

    public Key tooltipStyle() {
        return this.argument;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.TOOLTIP_STYLE;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.tooltipStyle(argument.toString());
        return item;
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.TOOLTIP_STYLE;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            String id = arg.toString();
            return new TooltipStyleModifier<>(Key.of(id));
        }
    }
}
