package net.momirealms.craftengine.bukkit.entity.furniture.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PaperFurnitureEventListener implements Listener {
    private final BukkitFurnitureManager manager;

    public PaperFurnitureEventListener(final BukkitFurnitureManager manager) {
        this.manager = manager;
    }

    // 所有开始被世界追踪的实体都会触发，包括磁盘恢复、主动放置、WorldEdit/NMS 添加。
    // 不是「区块加载之后」事件：普通 Paper 区块恢复中，它早于 ChunkLoadEvent/EntitiesLoadEvent。
    // LOWEST 只决定本事件内的监听顺序，无法抢在 ServerLevel 建立追踪器/发送生成包之前。
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityAddedToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay itemDisplay) {
            this.manager.handleFurnitureEntityAdded(itemDisplay);
        } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
            this.manager.removeCopiedColliderEntity(entity);
        }
    }

    // 覆盖 /kill、Entity.remove（WorldEdit 删除）、区块退出可见状态、跨世界等原因。
    // 不等同于死亡事件；ItemDisplay 不是 LivingEntity，不能靠 EntityDeathEvent 替代。
    // 普通区块卸载可能随后再收到 EntitiesUnloadEvent，manager 先撤销映射以保证只卸载一次。
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityRemovedFromWorld(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay itemDisplay) {
            this.manager.unloadFurnitureFromEntity(itemDisplay, false);
        } else if (CraftEngineFurniture.isCollisionEntity(entity)) {
            this.manager.unregisterColliderEntity(entity);
        }
    }

    // 以下是「某个玩家」开始/结束观察，不是家具本身的加载/卸载。
    // 单实体补载发生较晚时，最初的 PlayerTrackEntityEvent 可能尚查不到家具映射；
    // 补发 AddEntity 包修复网络侧显示，不会重新触发这个 Bukkit 玩家事件。
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTrackFurniture(PlayerTrackEntityEvent event) {
        if (event.getEntity() instanceof ItemDisplay furnitureEntity) {
            int entityId = furnitureEntity.getEntityId();
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(entityId);
            if (furniture == null) return;
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(event.getPlayer());
            if (serverPlayer == null) return;
            furniture.controller.onPlayerTrack(serverPlayer);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onUntrackFurniture(PlayerUntrackEntityEvent event) {
        if (event.getEntity() instanceof ItemDisplay furnitureEntity) {
            int entityId = furnitureEntity.getEntityId();
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(entityId);
            if (furniture == null) return;
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(event.getPlayer());
            if (serverPlayer == null) return;
            furniture.controller.onPlayerUntrack(serverPlayer);
        }
    }
}
