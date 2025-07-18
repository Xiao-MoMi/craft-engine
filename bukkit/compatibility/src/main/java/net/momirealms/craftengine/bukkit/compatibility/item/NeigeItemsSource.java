package net.momirealms.craftengine.bukkit.compatibility.item;

import net.momirealms.craftengine.core.item.ExternalItemSource;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pers.neige.neigeitems.manager.ItemManager;

import java.util.Optional;

public class NeigeItemsSource implements ExternalItemSource<ItemStack> {

    @Override
    public String plugin() {
        return "neigeitems";
    }

    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        return ItemManager.INSTANCE.getItemStack(id, Optional.ofNullable(context.player()).map(it -> (Player) it.platformPlayer()).orElse(null));
    }

    @Override
    public String id(ItemStack item) {
        return ItemManager.INSTANCE.getItemId(item);
    }
}
