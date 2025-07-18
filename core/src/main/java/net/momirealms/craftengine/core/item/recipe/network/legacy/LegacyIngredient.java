package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public class LegacyIngredient {
    private final Item<Object>[] items;

    public LegacyIngredient(Item<Object>[] items) {
        this.items = items;
    }

    public Item<?>[] items() {
        return items;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeArray(this.items, (byteBuf, item) -> CraftEngine.instance().itemManager().encode(byteBuf, item));
    }

    @SuppressWarnings("unchecked")
    public static LegacyIngredient read(FriendlyByteBuf buf) {
        Item<Object>[] items = buf.readArray(byteBuf -> CraftEngine.instance().itemManager().decode(byteBuf), Item.class);
        return new LegacyIngredient(items);
    }

    public void applyClientboundData(Player player) {
        ItemManager<Object> manager = CraftEngine.instance().itemManager();
        for (int i = 0; i < this.items.length; i++) {
            Item<Object> item = this.items[i];
            this.items[i] = manager.s2c(item, player);
        }
    }
}
