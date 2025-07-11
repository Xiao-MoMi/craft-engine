package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureExtraData;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CraftEngineFurniture {

    private CraftEngineFurniture() {}

    /**
     * Gets custom furniture by ID
     *
     * @param id id
     * @return the custom furniture
     */
    public static CustomFurniture byId(@NotNull Key id) {
        return BukkitFurnitureManager.instance().furnitureById(id).orElse(null);
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture place(Location location, Key furnitureId) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return place(location, furnitureId, furniture.getAnyAnchorType());
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param anchorType  anchor type
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture place(Location location, Key furnitureId, AnchorType anchorType) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureExtraData.builder().anchorType(anchorType).build(), true);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param anchorType anchor type
     * @return the loaded furniture
     */
    @NotNull
    public static BukkitFurniture place(Location location, CustomFurniture furniture, AnchorType anchorType) {
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureExtraData.builder().anchorType(anchorType).build(), true);
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param anchorType  anchor type
     * @param playSound   whether to play place sounds
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture place(Location location, Key furnitureId, AnchorType anchorType, boolean playSound) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureExtraData.builder().anchorType(anchorType).build(), playSound);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param anchorType anchor type
     * @param playSound  whether to play place sounds
     * @return the loaded furniture
     */
    @NotNull
    public static BukkitFurniture place(Location location, CustomFurniture furniture, AnchorType anchorType, boolean playSound) {
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureExtraData.builder().anchorType(anchorType).build(), playSound);
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
     * Check if an entity is a collision entity
     *
     * @param entity entity to check
     * @return is collision entity or not
     */
    public static boolean isCollisionEntity(@NotNull Entity entity) {
        Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(entity);
        return nmsEntity instanceof CollisionEntity;
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
    public static BukkitFurniture getLoadedFurnitureByBaseEntity(@NotNull Entity baseEntity) {
        return BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(baseEntity.getEntityId());
    }

    /**
     * Gets the base furniture by the seat entity
     *
     * @param seat seat entity
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture getLoadedFurnitureBySeat(@NotNull Entity seat) {
        Integer baseEntityId = seat.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER);
        if (baseEntityId == null) return null;
        return BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(baseEntityId);
    }

    /**
     * Removes furniture
     *
     * @param entity furniture base entity
     * @return success or not
     */
    public static boolean remove(@NotNull Entity entity) {
        if (!isFurniture(entity)) return false;
        BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(entity.getEntityId());
        if (furniture == null) return false;
        furniture.destroy();
        return true;
    }

    /**
     * Removes furniture, with more options
     *
     * @param entity furniture base entity
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     * @return success or not
     */
    public static boolean remove(@NotNull Entity entity,
                                 boolean dropLoot,
                                 boolean playSound) {
        if (!isFurniture(entity)) return false;
        BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(entity.getEntityId());
        if (furniture == null) return false;
        remove(furniture, (net.momirealms.craftengine.core.entity.player.Player) null, dropLoot, playSound);
        return true;
    }

    /**
     * Removes furniture, with more options
     *
     * @param entity furniture base entity
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     * @return success or not
     */
    public static boolean remove(@NotNull Entity entity,
                                 @Nullable Player player,
                                 boolean dropLoot,
                                 boolean playSound) {
        if (!isFurniture(entity)) return false;
        Furniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(entity.getEntityId());
        if (furniture == null) return false;
        remove(furniture, player, dropLoot, playSound);
        return true;
    }

    /**
     * Removes furniture by providing furniture instance
     *
     * @param furniture loaded furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */
    public static void remove(@NotNull Furniture furniture,
                              boolean dropLoot,
                              boolean playSound) {
        remove(furniture, (net.momirealms.craftengine.core.entity.player.Player) null, dropLoot, playSound);
    }

    /**
     * Removes furniture by providing furniture instance
     *
     * @param furniture loaded furniture
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */

    public static void remove(@NotNull Furniture furniture,
                              @Nullable Player player,
                              boolean dropLoot,
                              boolean playSound) {
        remove(furniture, player == null ? null : BukkitCraftEngine.instance().adapt(player), dropLoot, playSound);
    }

    /**
     * Removes furniture by providing furniture instance
     *
     * @param furniture loaded furniture
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */
    @SuppressWarnings("unchecked")
    public static void remove(@NotNull Furniture furniture,
                              @Nullable net.momirealms.craftengine.core.entity.player.Player player,
                              boolean dropLoot,
                              boolean playSound) {
        if (!furniture.isValid()) return;
        Location location = ((BukkitFurniture) furniture).dropLocation();
        furniture.destroy();
        LootTable<ItemStack> lootTable = (LootTable<ItemStack>) furniture.config().lootTable();
        World world = new BukkitWorld(location.getWorld());
        WorldPosition position = new WorldPosition(world, location.getX(), location.getY(), location.getZ());
        if (dropLoot && lootTable != null) {
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.POSITION, position)
                    .withParameter(DirectContextParameters.FURNITURE, furniture)
                    .withOptionalParameter(DirectContextParameters.FURNITURE_ITEM, furniture.extraData().item().orElse(null));
            if (player != null) {
                Item<?> itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                builder.withParameter(DirectContextParameters.PLAYER, player)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand);
            }
            List<Item<ItemStack>> items = lootTable.getRandomItems(builder.build(), world, player);
            for (Item<ItemStack> item : items) {
                world.dropItemNaturally(position, item);
            }
        }
        if (playSound) {
            world.playBlockSound(position, furniture.config().settings().sounds().breakSound());
        }
    }
}
