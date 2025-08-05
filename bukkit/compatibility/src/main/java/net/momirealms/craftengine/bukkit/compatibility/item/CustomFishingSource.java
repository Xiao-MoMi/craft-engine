package net.momirealms.craftengine.bukkit.compatibility.item;

import net.momirealms.craftengine.core.item.ExternalItemSource;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;
import net.momirealms.customfishing.api.mechanic.context.ContextKeys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CustomFishingSource implements ExternalItemSource<ItemStack> {
    @Override
    public String plugin() {
        return "customfishing";
    }

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        Context<Player> ctx = Context.player(
                (Player) Optional.ofNullable(context.player())
                        .map(net.momirealms.craftengine.core.entity.player.Player::platformPlayer)
                        .orElse(null)
        );
        return BukkitCustomFishingPlugin.getInstance().getItemManager().buildInternal(ctx.arg(ContextKeys.ID, id), id);
    }

    @Override
    public String id(ItemStack item) {
        return BukkitCustomFishingPlugin.getInstance().getItemManager().getCustomFishingItemID(item);
    }
}
