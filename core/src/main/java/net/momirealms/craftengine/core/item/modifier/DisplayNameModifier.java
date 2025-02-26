package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.minimessage.ImageTag;
import net.momirealms.craftengine.core.plugin.minimessage.NamedArgumentTag;
import net.momirealms.craftengine.core.plugin.minimessage.PlaceholderTag;
import net.momirealms.craftengine.core.plugin.minimessage.ShiftTag;
import net.momirealms.craftengine.core.util.AdventureHelper;

public class DisplayNameModifier<I> implements ItemModifier<I> {
    private final String argument;

    public DisplayNameModifier(String argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "display-name";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        item.displayName(AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(
                this.argument, ImageTag.INSTANCE, ShiftTag.INSTANCE, new PlaceholderTag(context.player()), new NamedArgumentTag(context.contexts()))));
    }
}
