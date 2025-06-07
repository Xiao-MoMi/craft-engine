package net.momirealms.craftengine.bukkit.entity.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class FurnitureEventListener implements Listener {
    private final BukkitFurnitureManager manager;

    public FurnitureEventListener(final BukkitFurnitureManager manager) {
        this.manager = manager;
    }

    /*
     * Load Entities
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntitiesLoadEarly(EntitiesLoadEvent event) {
        List<Entity> entities = event.getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleBaseEntityLoadEarly(itemDisplay);
                BukkitFurniture.injectFurnitureEntity(FastNMS.INSTANCE.method$CraftEntity$getHandle(itemDisplay));
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityLoadOnEntitiesLoad(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        List<Entity> entities = event.getWorld().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleBaseEntityLoadEarly(itemDisplay);
                BukkitFurniture.injectFurnitureEntity(FastNMS.INSTANCE.method$CraftEntity$getHandle(itemDisplay));
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityLoadOnEntitiesLoad(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityLoad(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay itemDisplay) {
            this.manager.handleBaseEntityLoadLate(itemDisplay, 0);
        } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
            this.manager.handleCollisionEntityLoadLate(entity, 0);
        }
    }

    /*
     * Unload Entities
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Entity[] entities = event.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay) {
                this.manager.handleBaseEntityUnload(entity);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityUnload(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        List<Entity> entities = event.getWorld().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay) {
                this.manager.handleBaseEntityUnload(entity);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityUnload(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityUnload(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay) {
            this.manager.handleBaseEntityUnload(entity);
        } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
            this.manager.handleCollisionEntityUnload(entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (this.manager.isSeatCarrierType(entity)) {
            this.manager.tryLeavingSeat(player, entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (this.manager.isSeatCarrierType(entity)) {
            this.manager.tryLeavingSeat(player, entity);
        }
    }

    // do not allow players to put item on seats
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInteractArmorStand(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked instanceof ArmorStand armorStand) {
            Integer baseFurniture = armorStand.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER);
            if (baseFurniture == null) return;
            event.setCancelled(true);
        }
    }
}
