package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.minimessage.ImageTag;
import net.momirealms.craftengine.core.plugin.minimessage.NamedArgumentTag;
import net.momirealms.craftengine.core.plugin.minimessage.PlaceholderTag;
import net.momirealms.craftengine.core.plugin.minimessage.ShiftTag;
import net.momirealms.craftengine.core.util.AdventureHelper;

import java.util.List;

public class LoreModifier<I> implements ItemModifier<I> {
    private final List<String> argument;

    public LoreModifier(List<String> argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "lore";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        item.lore(argument.stream().map(it -> AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(
                it, ImageTag.INSTANCE, ShiftTag.INSTANCE, new PlaceholderTag(context.player()), new NamedArgumentTag(context.contexts())))).toList());
    }
}
