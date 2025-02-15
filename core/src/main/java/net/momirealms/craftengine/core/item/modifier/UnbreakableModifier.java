package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;

public class UnbreakableModifier<I> implements ItemModifier<I> {
    private final boolean argument;

    public UnbreakableModifier(boolean argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "unbreakable";
    }

    @Override
    public void apply(Item<I> item, Player player) {
        item.unbreakable(argument);
    }
}
