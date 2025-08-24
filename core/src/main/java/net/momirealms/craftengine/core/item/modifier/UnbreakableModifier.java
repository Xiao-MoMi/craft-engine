package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

public class UnbreakableModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final boolean argument;

    public UnbreakableModifier(boolean argument) {
        this.argument = argument;
    }

    public boolean unbreakable() {
        return argument;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.UNBREAKABLE;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.unbreakable(this.argument);
        return item;
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.UNBREAKABLE;
    }

    @Override
    public @Nullable Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"Unbreakable"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "Unbreakable";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            boolean value = ResourceConfigUtils.getAsBoolean(arg, "unbreakable");
            return new UnbreakableModifier<>(value);
        }
    }
}
