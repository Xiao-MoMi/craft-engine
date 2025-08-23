package net.momirealms.craftengine.core.item.modifier.lore;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifiers;
import net.momirealms.craftengine.core.item.modifier.SimpleNetworkItemDataModifier;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

public final class OverwritableLoreModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final LoreModifier<I> loreModifier;

    public OverwritableLoreModifier(LoreModifier<I> loreModifier) {
        this.loreModifier = loreModifier;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.OVERWRITABLE_LORE;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(ComponentKeys.LORE)) {
                return item;
            }
        } else {
            if (item.hasTag("display", "Lore")) {
                return item;
            }
        }
        return this.loreModifier.apply(item, context);
    }

    @Override
    public Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.LORE;
    }

    @Override
    public Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"display", "Lore"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Lore";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {
        @Override
        public ItemDataModifier<I> create(Object arg) {
            LoreModifier<I> lore = LoreModifier.createLoreModifier(arg);
            return new OverwritableLoreModifier<>(lore);
        }
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(ComponentKeys.LORE)) {
                return item;
            }
        } else {
            if (item.hasTag("display", "Lore")) {
                return item;
            }
        }
        return SimpleNetworkItemDataModifier.super.prepareNetworkItem(item, context, networkData);
    }
}
