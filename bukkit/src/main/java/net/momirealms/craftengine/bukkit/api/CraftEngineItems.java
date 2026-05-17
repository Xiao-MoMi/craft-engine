package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CraftEngineItems {
    private CraftEngineItems() {}

    /**
     * Returns an unmodifiable map of all currently loaded custom items.
     * The map keys represent unique identifiers, and the values are the corresponding CustomItem instances.
     *
     * <p><strong>Important:</strong> Do not attempt to access this method during the onEnable phase
     * as it will be empty. Instead, listen for the {@code CraftEngineReloadEvent} and use this method
     * after the event is fired to obtain the complete item list.
     *
     * @return a non-null map containing all loaded custom items
     * @throws IllegalStateException if the BukkitItemManager instance is not available
     */
    @NotNull
    public static Map<Key, ItemDefinition> loadedItems() {
        return BukkitItemManager.instance().loadedItems();
    }

    /**
     * Gets a custom item by ID
     *
     * @param id id
     * @return the custom item
     */
    @Nullable
    public static BukkitItemDefinition byId(@NotNull final Key id) {
        return (BukkitItemDefinition) BukkitItemManager.instance().getItemDefinition(id).orElse(null);
    }

    /**
     * Gets a custom item by ID
     */
    @Nullable
    public static BukkitItemDefinition byId(@NotNull final String id) {
        if (id.contains(":")) {
            return byId(Key.of(id));
        } else {
            return (BukkitItemDefinition) BukkitItemManager.instance().getItemDefinitionByPath(id).orElse(null);
        }
    }

    /**
     * Gets a custom item by existing item stack
     *
     * @param itemStack item stack
     * @return the custom item
     */
    @Nullable
    public static BukkitItemDefinition byItemStack(@NotNull ItemStack itemStack) {
        if (ItemStackUtils.isEmpty(itemStack)) return null;
        return (BukkitItemDefinition) BukkitItemManager.instance().wrap(itemStack).getDefinition().orElse(null);
    }

    /**
     * Checks if an item is a custom one
     *
     * @param itemStack item stack
     * @return true if it's a custom item
     */
    public static boolean isCustomItem(@NotNull ItemStack itemStack) {
        if (ItemStackUtils.isEmpty(itemStack)) return false;
        return BukkitItemManager.instance().wrap(itemStack).isCustomItem();
    }

    /**
     * Gets custom item id from item stack
     *
     * @param itemStack item stack
     * @return the custom id, null if it's not a custom one
     */
    @Nullable
    public static Key getCustomItemId(@NotNull ItemStack itemStack) {
        if (ItemStackUtils.isEmpty(itemStack)) return null;
        return BukkitItemManager.instance().wrap(itemStack).customId().orElse(null);
    }
}
