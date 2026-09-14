package net.momirealms.craftengine.bukkit.entity.furniture.listener;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.api.event.FurnitureHitEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.behavior.GlowingFurnitureBehaviorTemplate;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BukkitItemUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDebugStickState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.customdata.FurnitureDebugStickData;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSystemChatPacketProxy;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.world.*;

import java.util.List;
import java.util.Optional;

/** 区块/世界批量生命周期；单实体添加和 /kill 等补充入口见 PaperFurnitureEventListener。 */
@SuppressWarnings("DuplicatedCode")
public final class FurnitureEventListener implements Listener {
    private static final String DEBUG_STICK_TAG = "craftengine:debug_stick_state";
    private final BukkitFurnitureManager manager;
    private final BukkitWorldManager worldManager;

    public FurnitureEventListener(final BukkitFurnitureManager manager, final BukkitWorldManager worldManager) {
        this.manager = manager;
        this.worldManager = worldManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnitureTeleport(EntityTeleportEvent event) {
        if (event.getTo() != null && event.getEntity() instanceof ItemDisplay display) {
            this.manager.handleFurnitureTeleport(display);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onWorldSave(WorldSaveEvent event) {
        // 元数据 PDC 是存档来源；行为内部状态变脏时，先写回实体再由服务端序列化。
        List<ItemDisplay> entities = (List<ItemDisplay>) event.getWorld().getEntitiesByClass(ItemDisplay.class);
        for (int i = 0, size = entities.size(); i < size; i++) {
            ItemDisplay entity = entities.get(i);
            BukkitFurniture furniture = this.manager.loadedFurnitureByMetaEntityId(entity.getEntityId());
            if (furniture != null) {
                furniture.saveIfDirty();
            }
        }
    }

    // Paper 的普通加载：单实体 Add 事件 -> ChunkLoadEvent -> EntitiesLoadEvent。
    // Spigot 的批量事件来自 PersistentEntitySectionManager 完成实体读取，不能照搬 Paper 区块时序。
    // LOWEST 表示尽早处理本批实体，不表示本方法早于另一个事件类型的 LOWEST。
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        Chunk chunk = event.getChunk();
        if (!chunk.isLoaded()) {
            return;
        }
        // 只在本次同步批量处理内共享状态检查；不能把 runner 保留到下一批或下一 tick。
        BukkitFurnitureManager.SafeEntityOperationRunner operationRunner = this.manager.newEntityOperationRunner(chunk);
        List<Entity> entities = event.getEntities();
        for (int i = 0, size = entities.size(); i < size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.restoreFurnitureFromEntity(itemDisplay, operationRunner);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.removeStaleColliderEntity(entity);
            }
        }
        CEWorld world = BukkitAdaptor.adapt(event.getWorld()).storageWorld();
        CEChunk ceChunk = world.getChunkAtIfLoaded(chunk.getX(), chunk.getZ());
        if (ceChunk != null) {
            // 在本批恢复结束后才允许单实体补载入口接管后续外部生成。
            // 这是 CE 的阶段标记，不是 NMS 实体的 valid/persistent 状态。
            ceChunk.setEntitiesLoaded(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onWorldLoad(WorldLoadEvent event) {
        // BukkitWorldManager 的 LOWEST 先建立 CE 世界/区块，再扫描可能早于监听器存在的实体。
        // 这是补扫描，实体可能已经登记；去重必须由 manager 统一处理。
        List<Entity> entities = event.getWorld().getEntities();
        for (int i = 0, size = entities.size(); i < size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.restoreFurnitureFromEntity(itemDisplay);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.removeStaleColliderEntity(entity);
            }
        }
    }

    // Paper 普通卸载：单实体 Remove -> EntitiesUnloadEvent -> ChunkUnloadEvent。
    // 单实体回调可能因 section 正在切换而不能删除 Collider，这里承担批量清理。
    // 非持久化的 Collider 不应依赖原版实体存档/卸载流程替我们销毁。
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        List<Entity> entities = event.getEntities();
        for (int i = 0, size = entities.size(); i < size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.unloadFurnitureFromEntity(itemDisplay, false);
            } else if (CraftEngineFurniture.isCollisionEntity(entity)) {
                this.manager.unregisterColliderEntity(entity);
                entity.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        // WorldUnload 也可能与区块/单实体卸载重叠；元数据卸载仍以 manager 映射去重。
        // HIGHEST 在 BukkitWorldManager 的 MONITOR 移除 CE 世界之前清理家具。
        GlowingFurnitureBehaviorTemplate.LIGHT_DATA.remove(event.getWorld().getUID());
        List<Entity> entities = event.getWorld().getEntities();
        for (int i = 0, size = entities.size(); i < size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.unloadFurnitureFromEntity(itemDisplay, false);
            } else if (CraftEngineFurniture.isCollisionEntity(entity)) {
                this.manager.unregisterColliderEntity(entity);
                entity.remove();
            }
        }
    }

    /*


    杂项


     */
    @SuppressWarnings("DuplicatedCode")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFurnitureHitWithDebugStick(FurnitureHitEvent event) {
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = BukkitAdaptor.adapt(bukkitPlayer);
        if (player == null) return;

        // 触发家具点击
        BukkitFurniture furniture = event.furniture();
        InteractionResult result = furniture.controller.onPlayerHit(player, event.hitBox());
        if (InteractionResult.SUCCESS_AND_CANCEL.equals(result)) {
            event.setCancelled(true);
            return;
        }

        // 调试棒操作
        Item itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BukkitItemUtils.isDebugStick(itemInHand)) return;
        if (!(player.canInstabuild() && player.hasPermission("minecraft.debugstick")) && !player.hasPermission("minecraft.debugstick.always")) {
            return;
        }
        event.setCancelled(true);

        FurnitureDebugStickData debugStickData = Optional.ofNullable(itemInHand.getCustomData(FurnitureDebugStickData.class, DEBUG_STICK_TAG, "furniture")).orElseGet(FurnitureDebugStickData::new);
        FurnitureDebugStickState state = debugStickData.state;
        state = player.isSecondaryUseActive() ? state.previous() : state.next();
        debugStickData.state = state;
        itemInHand.setCustomData(debugStickData, DEBUG_STICK_TAG, "furniture");
        Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(
                ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.select")
                        .arguments(
                                Component.text(state.propertyName()),
                                Component.text(state.format(furniture))
                        )), true);
        player.sendPacket(systemChatPacket, false);
    }

    @SuppressWarnings("DuplicatedCode")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteractFurniture(FurnitureInteractEvent event) {
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = BukkitAdaptor.adapt(bukkitPlayer);
        if (player == null) return;
        Item itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BukkitItemUtils.isDebugStick(itemInHand)) return;
        if (!(player.canInstabuild() && player.hasPermission("minecraft.debugstick")) && !player.hasPermission("minecraft.debugstick.always")) {
            return;
        }

        FurnitureDebugStickData debugStickData = Optional.ofNullable(itemInHand.getCustomData(FurnitureDebugStickData.class, DEBUG_STICK_TAG, "furniture")).orElseGet(FurnitureDebugStickData::new);
        FurnitureDebugStickState state = debugStickData.state;
        BukkitFurniture furniture = event.furniture();
        state.handler().onInteract(player.isSecondaryUseActive(), furniture, (s1, s2) -> {
            Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(
                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.update")
                            .arguments(
                                    Component.text(s1),
                                    Component.text(s2)
                            )), true);
            player.sendPacket(systemChatPacket, false);
        }, () -> {
            Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(
                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.empty").arguments(Component.text(furniture.id().asString()))), true);
            player.sendPacket(systemChatPacket, false);
        });
    }
}
