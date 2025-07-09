package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public interface Equipment {

    Key assetId();

    Key type();

    <I> List<ItemDataModifier<I>> modifiers();
}
