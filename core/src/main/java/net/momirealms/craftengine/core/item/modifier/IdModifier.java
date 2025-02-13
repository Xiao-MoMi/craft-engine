package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public class IdModifier<I> implements ItemModifier<I> {
    private final Key argument;

    public IdModifier(Key argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "tag";
    }

    @Override
    public void apply(Item<I> item, Player player) {
        item.setTag(argument.toString(), "craftengine:id");
    }
}
