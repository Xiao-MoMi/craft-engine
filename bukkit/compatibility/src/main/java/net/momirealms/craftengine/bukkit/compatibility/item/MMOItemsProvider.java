package net.momirealms.craftengine.bukkit.compatibility.item;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.momirealms.craftengine.core.item.ExternalItemProvider;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public class MMOItemsProvider implements ExternalItemProvider<ItemStack> {

    @Override
    public String plugin() {
        return "MMOItems";
    }

    @Override
    public @Nullable ItemStack build(String id, ItemBuildContext context) {
        String[] split = id.split(":", 2);
        MMOItem mmoItem = MMOItems.plugin.getMMOItem(Type.get(split[0]), split[1].toUpperCase());
        return mmoItem == null ? new ItemStack(Material.AIR) : requireNonNull(mmoItem.newBuilder().build());
    }

    @Override
    public String id(ItemStack item) {
        return MMOItems.getType(item) + ":" + MMOItems.getID(item);
    }
}
