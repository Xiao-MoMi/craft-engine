package net.momirealms.craftengine.bukkit.compatibility.item;

import io.lumine.mythic.bukkit.MythicBukkit;
import net.momirealms.craftengine.core.item.ExternalItemProvider;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MythicMobsProvider implements ExternalItemProvider<ItemStack> {
    private MythicBukkit mythicBukkit;

    @Override
    public String plugin() {
        return "MythicMobs";
    }

    @Nullable
    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        if (mythicBukkit == null || mythicBukkit.isClosed()) {
            this.mythicBukkit = MythicBukkit.inst();
        }
        return mythicBukkit.getItemManager().getItemStack(id);
    }

    @Override
    public String id(ItemStack item) {
        if (mythicBukkit == null || mythicBukkit.isClosed()) {
            this.mythicBukkit = MythicBukkit.inst();
        }
        return mythicBukkit.getItemManager().getMythicTypeFromItem(item);
    }
}
