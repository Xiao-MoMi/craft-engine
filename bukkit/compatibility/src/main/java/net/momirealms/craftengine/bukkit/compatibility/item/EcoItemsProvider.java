package net.momirealms.craftengine.bukkit.compatibility.item;

import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.EcoItems;
import net.momirealms.craftengine.core.item.ExternalItemProvider;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EcoItemsProvider implements ExternalItemProvider<ItemStack> {
    @Override
    public String plugin() {
        return "EcoItems";
    }

    @Nullable
    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        return Optional.ofNullable(EcoItems.INSTANCE.getByID(id))
                .map(EcoItem::getItemStack)
                .orElse(null);
    }
}
