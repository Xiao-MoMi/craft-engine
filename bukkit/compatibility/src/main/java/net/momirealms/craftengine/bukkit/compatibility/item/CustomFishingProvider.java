package net.momirealms.craftengine.bukkit.compatibility.item;

import net.momirealms.craftengine.core.item.ExternalItemProvider;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CustomFishingProvider implements ExternalItemProvider<ItemStack> {
    @Override
    public String plugin() {
        return "CustomFishing";
    }

    @Nullable
    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        return BukkitCustomFishingPlugin.getInstance().getItemManager()
                .buildInternal(Context.player(((Player) context.player().platformPlayer())), id);
    }
}
