package net.momirealms.craftengine.bukkit.entity.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.api.event.FurnitureHitEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BukkitItemUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDebugStickState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FurnitureEventListener implements Listener {
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
        List<Entity> entities = event.getEntities();
        for (Entity entity : entities) {
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
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleMetaEntityDuringChunkLoad(itemDisplay);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityDuringChunkLoad(entity);
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
                this.manager.handleMetaEntityUnload(itemDisplay);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityUnload(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        List<Entity> entities = event.getWorld().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemDisplay itemDisplay) {
                this.manager.handleMetaEntityUnload(itemDisplay);
            } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                this.manager.handleCollisionEntityUnload(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityUnload(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemDisplay itemDisplay) {
            this.manager.handleMetaEntityUnload(itemDisplay);
        } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
            this.manager.handleCollisionEntityUnload(entity);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFurniturePreBreak(FurnitureHitEvent event) {
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = BukkitAdaptors.adapt(bukkitPlayer);
        if (player == null) return;
        Item<ItemStack> itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BukkitItemUtils.isDebugStick(itemInHand)) return;
        if (!(player.canInstabuild() && player.hasPermission("minecraft.debugstick")) && !player.hasPermission("minecraft.debugstick.always")) {
            return;
        }
        event.setCancelled(true);
        BukkitFurniture furniture = event.furniture();
        Object storedData = itemInHand.getJavaTag("craftengine:debug_stick_state");
        if (storedData == null) storedData = new HashMap<>();
        if (storedData instanceof Map<?,?> map) {
            Map<String, Object> data = new HashMap<>(MiscUtils.castToMap(map, false));
            FurnitureDebugStickState state = ResourceConfigUtils.getAsEnum(data.get("furniture"), FurnitureDebugStickState.class, FurnitureDebugStickState.VARIANT);
            state = player.isSecondaryUseActive() ? state.previous() : state.next();
            String propertyName = state.name().toLowerCase(Locale.ROOT);
            data.put("furniture", propertyName);
            itemInHand.setTag(data, "craftengine:debug_stick_state");
            Object systemChatPacket = FastNMS.INSTANCE.constructor$ClientboundSystemChatPacket(
                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.select")
                            .arguments(
                                    Component.text(propertyName),
                                    Component.text(state.format(furniture))
                            )), true);
            player.sendPacket(systemChatPacket, false);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteractFurniture(FurnitureInteractEvent event) {
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = BukkitAdaptors.adapt(bukkitPlayer);
        if (player == null) return;
        Item<ItemStack> itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BukkitItemUtils.isDebugStick(itemInHand)) return;
        if (!(player.canInstabuild() && player.hasPermission("minecraft.debugstick")) && !player.hasPermission("minecraft.debugstick.always")) {
            return;
        }
        Object storedData = itemInHand.getJavaTag("craftengine:debug_stick_state");
        if (storedData == null) storedData = new HashMap<>();
        if (storedData instanceof Map<?,?> map) {
            Map<String, Object> data = new HashMap<>(MiscUtils.castToMap(map, false));
            FurnitureDebugStickState state = ResourceConfigUtils.getAsEnum(data.get("furniture"), FurnitureDebugStickState.class, FurnitureDebugStickState.VARIANT);
            BukkitFurniture furniture = event.furniture();
            state.handler().onInteract(player.isSecondaryUseActive(), furniture, (s1, s2) -> {
                Object systemChatPacket = FastNMS.INSTANCE.constructor$ClientboundSystemChatPacket(
                        ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.update")
                                .arguments(
                                        Component.text(s1),
                                        Component.text(s2)
                                )), true);
                player.sendPacket(systemChatPacket, false);
            }, () -> {
                Object systemChatPacket = FastNMS.INSTANCE.constructor$ClientboundSystemChatPacket(
                        ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.empty").arguments(Component.text(furniture.id().asString()))), true);
                player.sendPacket(systemChatPacket, false);
            });
        }
    }
}
