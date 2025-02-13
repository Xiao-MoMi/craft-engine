package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;

public class CustomModelDataModifier<I> implements ItemModifier<I> {
    private final int argument;

    public CustomModelDataModifier(int argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "custom-model-data";
    }

    @Override
    public void apply(Item<I> item, Player player) {
        item.customModelData(argument);
    }
}
