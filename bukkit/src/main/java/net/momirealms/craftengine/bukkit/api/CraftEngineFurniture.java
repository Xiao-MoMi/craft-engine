package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CraftEngineFurniture {

    /**
     * Gets custom furniture by ID
     *
     * @param id id
     * @return the custom furniture
     */
    public static CustomFurniture byId(@NotNull Key id) {
        return BukkitFurnitureManager.instance().getFurniture(id).orElse(null);
    }

    /**
     * Places furniture at the certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param anchorType  anchor type
     * @return the loaded furniture
     */
    @Nullable
    public static LoadedFurniture place(Location location, Key furnitureId, AnchorType anchorType) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return BukkitFurnitureManager.instance().place(furniture, location, anchorType, true);
    }

    /**
     * Places furniture at the certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param anchorType anchor type
     * @return the loaded furniture
     */
    @NotNull
    public static LoadedFurniture place(Location location, CustomFurniture furniture, AnchorType anchorType) {
        return BukkitFurnitureManager.instance().place(furniture, location, anchorType, true);
    }

    /**
     * Places furniture at the certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param anchorType  anchor type
     * @param playSound   whether to play place sounds
     * @return the loaded furniture
     */
    @Nullable
    public static LoadedFurniture place(Location location, Key furnitureId, AnchorType anchorType, boolean playSound) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return BukkitFurnitureManager.instance().place(furniture, location, anchorType, playSound);
    }

    /**
     * Places furniture at the certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param anchorType anchor type
     * @param playSound  whether to play place sounds
     * @return the loaded furniture
     */
    @NotNull
    public static LoadedFurniture place(Location location, CustomFurniture furniture, AnchorType anchorType, boolean playSound) {
        return BukkitFurnitureManager.instance().place(furniture, location, anchorType, playSound);
    }

    /**
     * Check if an entity is a piece of furniture
     *
     * @param entity entity to check
     * @return is furniture or not
     */
    public static boolean isFurniture(@NotNull Entity entity) {
        String furnitureId = entity.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING);
        return furnitureId != null;
    }

    /**
     * Check if an entity is a seat
     *
     * @param entity entity to check
     * @return is seat or not
     */
    public static boolean isSeat(@NotNull Entity entity) {
        Integer baseEntityId = entity.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER);
        return baseEntityId != null;
    }

    /**
     * Gets the base furniture by the base entity
     *
     * @param baseEntity base entity
     * @return the loaded furniture
     */
    @Nullable
    public static LoadedFurniture getLoadedFurnitureByBaseEntity(@NotNull Entity baseEntity) {
        return BukkitFurnitureManager.instance().getLoadedFurnitureByBaseEntityId(baseEntity.getEntityId());
    }

    /**
     * Gets the base furniture by the seat entity
     *
     * @param seat seat entity
     * @return the loaded furniture
     */
    @Nullable
    public static LoadedFurniture getLoadedFurnitureBySeat(@NotNull Entity seat) {
        Integer baseEntityId = seat.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER);
        if (baseEntityId == null) return null;
        return BukkitFurnitureManager.instance().getLoadedFurnitureByBaseEntityId(baseEntityId);
    }

    /**
     * Removes furniture
     *
     * @param furniture furniture base entity
     * @return success or not
     */
    public static boolean remove(@NotNull Entity furniture) {
        if (!isFurniture(furniture)) return false;
        LoadedFurniture loadedFurniture = BukkitFurnitureManager.instance().getLoadedFurnitureByBaseEntityId(furniture.getEntityId());
        if (loadedFurniture == null) return false;
        loadedFurniture.destroy();
        return true;
    }

    /**
     * Removes furniture, with more options
     *
     * @param furniture furniture base entity
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     * @return success or not
     */
    public static boolean remove(@NotNull Entity furniture,
                                 boolean dropLoot,
                                 boolean playSound) {
        if (!isFurniture(furniture)) return false;
        LoadedFurniture loadedFurniture = BukkitFurnitureManager.instance().getLoadedFurnitureByBaseEntityId(furniture.getEntityId());
        if (loadedFurniture == null) return false;
        remove(loadedFurniture, (net.momirealms.craftengine.core.entity.player.Player) null, dropLoot, playSound);
        return true;
    }

    /**
     * Removes furniture, with more options
     *
     * @param furniture furniture base entity
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     * @return success or not
     */
    public static boolean remove(@NotNull Entity furniture,
                                 @Nullable Player player,
                                 boolean dropLoot,
                                 boolean playSound) {
        if (!isFurniture(furniture)) return false;
        LoadedFurniture loadedFurniture = BukkitFurnitureManager.instance().getLoadedFurnitureByBaseEntityId(furniture.getEntityId());
        if (loadedFurniture == null) return false;
        remove(loadedFurniture, player, dropLoot, playSound);
        return true;
    }

    /**
     * Removes furniture by providing a plugin furniture instance
     *
     * @param loadedFurniture loaded furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */
    public static void remove(@NotNull LoadedFurniture loadedFurniture,
                              boolean dropLoot,
                              boolean playSound) {
        remove(loadedFurniture, (net.momirealms.craftengine.core.entity.player.Player) null, dropLoot, playSound);
    }

    /**
     * Removes furniture by providing a plugin furniture instance
     *
     * @param loadedFurniture loaded furniture
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */

    public static void remove(@NotNull LoadedFurniture loadedFurniture,
                              @Nullable Player player,
                              boolean dropLoot,
                              boolean playSound) {
        remove(loadedFurniture, player == null ? null : BukkitCraftEngine.instance().adapt(player), dropLoot, playSound);
    }

    /**
     * Removes furniture by providing a plugin furniture instance
     *
     * @param loadedFurniture loaded furniture
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */
    @SuppressWarnings("unchecked")
    public static void remove(@NotNull LoadedFurniture loadedFurniture,
                              @Nullable net.momirealms.craftengine.core.entity.player.Player player,
                              boolean dropLoot,
                              boolean playSound) {
        Location location = loadedFurniture.location();
        loadedFurniture.destroy();
        LootTable<ItemStack> lootTable = (LootTable<ItemStack>) loadedFurniture.furniture().lootTable();
        Vec3d vec3d = LocationUtils.toVec3d(location);
        World world = new BukkitWorld(location.getWorld());
        if (dropLoot && lootTable != null) {
            ContextHolder.Builder builder = ContextHolder.builder();
            builder.withParameter(LootParameters.LOCATION, vec3d);
            builder.withParameter(LootParameters.WORLD, world);
            if (player != null) {
                builder.withParameter(LootParameters.PLAYER, player);
                builder.withParameter(LootParameters.TOOL, player.getItemInHand(InteractionHand.MAIN_HAND));
            }
            List<Item<ItemStack>> items = lootTable.getRandomItems(builder.build(), world);
            for (Item<ItemStack> item : items) {
                world.dropItemNaturally(vec3d, item);
            }
        }
        if (playSound) {
            world.playBlockSound(vec3d, loadedFurniture.furniture().settings().sounds().breakSound(), 1f, 1f);
        }
    }
}
