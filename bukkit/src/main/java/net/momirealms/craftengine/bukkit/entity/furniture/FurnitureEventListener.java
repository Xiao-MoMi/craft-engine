package net.momirealms.craftengine.bukkit.entity.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.FurnitureHitEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.*;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class FurnitureEventListener implements Listener {
    private static final String DEBUG_STICK_TAG = "craftengine:debug_stick_state";
    private final BukkitFurnitureManager manager;
    private final BukkitWorldManager worldManager;

    public FurnitureEventListener(final BukkitFurnitureManager manager, final BukkitWorldManager worldManager) {
        this.manager = manager;
        this.worldManager = worldManager;
    }

    /*
     * Load Entities
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntitiesLoadEarly(EntitiesLoadEvent event) {
        if (!event.getChunk().isLoaded()) {
            return;
        }
        List<Entity> entities = event.getEntities();
        for (int i = 0, size = entities.size(); i < size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleMetaEntityDuringChunkLoad(itemDisplay);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityDuringChunkLoad(entity);
            }
        }
        CEWorld world = this.worldManager.getWorld(event.getWorld());
        CEChunk ceChunk = world.getChunkAtIfLoaded(event.getChunk().getChunkKey());
        if (ceChunk != null) {
            ceChunk.setEntitiesLoaded(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onWorldLoad(WorldLoadEvent event) {
        List<Entity> entities = event.getWorld().getEntities();
        for (int i = 0, size = entities.size(); i < size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleMetaEntityDuringChunkLoad(itemDisplay);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityDuringChunkLoad(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onWorldSave(WorldSaveEvent event) {
        List<ItemDisplay> entities = (List<ItemDisplay>) event.getWorld().getEntitiesByClass(ItemDisplay.class);
        for (int i = 0, size = entities.size(); i < size; i++) {
            ItemDisplay entity = entities.get(i);
            BukkitFurniture furniture = this.manager.loadedFurnitureByMetaEntityId(entity.getEntityId());
            if (furniture != null) {
                furniture.saveIfDirty();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityLoad(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay itemDisplay) {
            this.manager.handleMetaEntityAfterChunkLoad(itemDisplay);
        } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
            this.manager.handleCollisionEntityAfterChunkLoad(entity);
        }
    }

    /*
     * Unload Entities
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Entity[] entities = event.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleMetaEntityUnload(itemDisplay, false);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityUnload(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        GlowingFurnitureBehaviorTemplate.LIGHT_DATA.remove(event.getWorld().getUID());
        List<Entity> entities = event.getWorld().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleMetaEntityUnload(itemDisplay, false);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityUnload(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityUnload(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay itemDisplay) {
            this.manager.handleMetaEntityUnload(itemDisplay, false);
        } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
            this.manager.handleCollisionEntityUnload(entity);
        }
    }

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
