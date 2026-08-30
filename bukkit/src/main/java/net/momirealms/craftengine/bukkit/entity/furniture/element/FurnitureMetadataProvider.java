package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.furniture.data.ItemPatch;
import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@FunctionalInterface
public interface FurnitureMetadataProvider {

    List<Object> apply(Player player, @Nullable FurnitureDataResolver<ItemPatch> itemPatch, boolean force);
}
