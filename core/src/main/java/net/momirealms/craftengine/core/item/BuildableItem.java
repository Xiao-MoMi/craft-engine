package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public interface BuildableItem {

    Key id();

    Item buildItem(ItemBuildContext context, int count);

    default Item buildItem(@Nullable Player player) {
        return buildItem(ItemBuildContext.of(player));
    }

    default Item buildItem(ItemBuildContext context) {
        return buildItem(context, 1);
    }
}
