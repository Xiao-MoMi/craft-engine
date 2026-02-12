package net.momirealms.craftengine.bukkit.entity.seat;

import net.momirealms.craftengine.bukkit.entity.furniture.DismountListener1_20;
import net.momirealms.craftengine.bukkit.entity.furniture.DismountListener1_20_3;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.seat.SeatManager;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class BukkitSeatManager implements SeatManager, Listener {
    private static BukkitSeatManager instance;
    public static final NamespacedKey SEAT_KEY = KeyUtils.toNamespacedKey(SeatManager.SEAT_KEY);
    public static final NamespacedKey SEAT_EXTRA_DATA_KEY = KeyUtils.toNamespacedKey(SeatManager.SEAT_EXTRA_DATA_KEY);
    private final BukkitCraftEngine plugin;
    private final Listener dismountListener;

    public BukkitSeatManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.dismountListener = VersionHelper.isOrAbove1_20_3() ? new DismountListener1_20_3(this::handleDismount) : new DismountListener1_20(this::handleDismount);
        instance = this;
    }

    public CompoundTag getSeatExtraData(Entity entity) {
        if (!isSeatEntityType(entity)) {
            throw new IllegalArgumentException("Entity is not a seat");
        }
        byte[] bytes = entity.getPersistentDataContainer().get(SEAT_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY);
        try {
            return NBT.fromBytes(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read extra data from seat", e);
        }
    }

    private void handleDismount(Player player, @NotNull Entity dismounted) {
        if (!isSeatEntityType(dismounted)) return;
        tryLeavingSeat(player, dismounted);
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.dismountListener, this.plugin.javaPlugin());
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this.dismountListener);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                tryLeavingSeat(player, vehicle);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (this.isSeatEntityType(entity)) {
            this.tryLeavingSeat(player, entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (this.isSeatEntityType(entity)) {
            this.tryLeavingSeat(player, entity);
        }
    }

    // do not allow players to put item on seats
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInteractArmorStand(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked instanceof ArmorStand armorStand) {
            if (!armorStand.getPersistentDataContainer().has(SEAT_KEY)) return;
            event.setCancelled(true);
        }
    }

    protected boolean isSeatEntityType(Entity entity) {
        return (entity instanceof ArmorStand || entity instanceof ItemDisplay);
    }

    protected void tryLeavingSeat(@NotNull Player player, @NotNull Entity seat) {
        boolean isSeat = seat.getPersistentDataContainer().has(SEAT_KEY);
        if (!isSeat) return;
        Location location = seat.getLocation();
        if (seat instanceof ArmorStand) {
            location.add(0, 0.3875,0);
        } else {
            location.add(0,-0.35,0);
        }
        seat.remove();
        location.add(0, 0.301, 0); // 防止座椅较低卡进地下
        EntityUtils.safeDismount(player, location);
    }

    public static BukkitSeatManager instance() {
        return instance;
    }
}
