package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public class ItemModelModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key data;

    public ItemModelModifier(Key data) {
        this.data = data;
    }

    public Key data() {
        return data;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.ITEM_MODEL;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        return item.itemModel(this.data.asString());
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.ITEM_MODEL;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            String id = arg.toString();
            return new ItemModelModifier<>(Key.of(id));
        }
    }
}
