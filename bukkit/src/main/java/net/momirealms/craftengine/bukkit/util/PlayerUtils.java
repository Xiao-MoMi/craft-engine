package net.momirealms.craftengine.bukkit.util;

import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    public static void dropItem(@NotNull Player player, @NotNull ItemStack itemStack, boolean retainOwnership, boolean noPickUpDelay, boolean throwRandomly) {
        requireNonNull(player, "player");
        requireNonNull(itemStack, "itemStack");
        Location location = player.getLocation().clone();
        Item item = player.getWorld().dropItem(player.getEyeLocation().clone().subtract(new Vector(0, 0.3, 0)), itemStack);
        item.setPickupDelay(noPickUpDelay ? 0 : 40);
        item.setOwner(player.getUniqueId());
        if (retainOwnership) {
            item.setThrower(player.getUniqueId());
        }
        if (throwRandomly) {
            double d1 = RandomUtils.generateRandomDouble(0, 1) * 0.5f;
            double d2 = RandomUtils.generateRandomDouble(0, 1) * (Math.PI * 2);
            item.setVelocity(new Vector(-Math.sin(d2) * d1, 0.2f, Math.cos(d2) * d1));
        } else {
            double d1 = Math.sin(location.getPitch() * (Math.PI / 180));
            double d2 = RandomUtils.generateRandomDouble(0, 0.02);
            double d3 = RandomUtils.generateRandomDouble(0, 1) * (Math.PI * 2);
            Vector vector = location.getDirection().multiply(0.3).setY(-d1 * 0.3 + 0.1 + (RandomUtils.generateRandomDouble(0, 1) - RandomUtils.generateRandomDouble(0, 1)) * 0.1);
            vector.add(new Vector(Math.cos(d3) * d2, 0, Math.sin(d3) * d2));
            item.setVelocity(vector);
        }
    }

    public static int putItemsToInventory(Inventory inventory, ItemStack itemStack, int amount) {
        ItemMeta meta = itemStack.getItemMeta();
        int maxStackSize = itemStack.getMaxStackSize();
        for (ItemStack other : inventory.getStorageContents()) {
            if (other != null) {
                if (other.getType() == itemStack.getType() && other.getItemMeta().equals(meta)) {
                    if (other.getAmount() < maxStackSize) {
                        int delta = maxStackSize - other.getAmount();
                        if (amount > delta) {
                            other.setAmount(maxStackSize);
                            amount -= delta;
                        } else {
                            other.setAmount(amount + other.getAmount());
                            return 0;
                        }
                    }
                }
            }
        }

        if (amount > 0) {
            for (ItemStack other : inventory.getStorageContents()) {
                if (other == null) {
                    if (amount > maxStackSize) {
                        amount -= maxStackSize;
                        ItemStack cloned = itemStack.clone();
                        cloned.setAmount(maxStackSize);
                        inventory.addItem(cloned);
                    } else {
                        ItemStack cloned = itemStack.clone();
                        cloned.setAmount(amount);
                        inventory.addItem(cloned);
                        return 0;
                    }
                }
            }
        }

        return amount;
    }

    public static int giveItem(Player player, ItemStack itemStack, int amount) {
        PlayerInventory inventory = player.getInventory();
        ItemMeta meta = itemStack.getItemMeta();
        int maxStackSize = itemStack.getMaxStackSize();
        if (amount > maxStackSize * 100) {
            amount = maxStackSize * 100;
        }
        int actualAmount = amount;
        for (ItemStack other : inventory.getStorageContents()) {
            if (other != null) {
                if (other.getType() == itemStack.getType() && other.getItemMeta().equals(meta)) {
                    if (other.getAmount() < maxStackSize) {
                        int delta = maxStackSize - other.getAmount();
                        if (amount > delta) {
                            other.setAmount(maxStackSize);
                            amount -= delta;
                        } else {
                            other.setAmount(amount + other.getAmount());
                            return actualAmount;
                        }
                    }
                }
            }
        }
        if (amount > 0) {
            for (ItemStack other : inventory.getStorageContents()) {
                if (other == null) {
                    if (amount > maxStackSize) {
                        amount -= maxStackSize;
                        ItemStack cloned = itemStack.clone();
                        cloned.setAmount(maxStackSize);
                        inventory.addItem(cloned);
                    } else {
                        ItemStack cloned = itemStack.clone();
                        cloned.setAmount(amount);
                        inventory.addItem(cloned);
                        return actualAmount;
                    }
                }
            }
        }

        if (amount > 0) {
            for (int i = 0; i < amount / maxStackSize; i++) {
                ItemStack cloned = itemStack.clone();
                cloned.setAmount(maxStackSize);
                player.getWorld().dropItem(player.getLocation(), cloned);
            }
            int left = amount % maxStackSize;
            if (left != 0) {
                ItemStack cloned = itemStack.clone();
                cloned.setAmount(left);
                player.getWorld().dropItem(player.getLocation(), cloned);
            }
        }

        return actualAmount;
    }

    public static void sendTotemAnimation(Player player, ItemStack totem) {
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        List<Object> packets = new ArrayList<>();
        try {
            Object previousItem = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(offhandItem);
            Object totemItem = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(totem);

            Object packet1 = NetworkReflections.constructor$ClientboundSetEquipmentPacket
                    .newInstance(player.getEntityId(), List.of(Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, totemItem)));
            Object packet2 = NetworkReflections.constructor$ClientboundEntityEventPacket
                    .newInstance(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player), (byte) 35);
            Object packet3 = NetworkReflections.constructor$ClientboundSetEquipmentPacket
                    .newInstance(player.getEntityId(), List.of(Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, previousItem)));
            packets.add(packet1);
            packets.add(packet2);
            packets.add(packet3);

            Object bundlePacket = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);
            BukkitNetworkManager.instance().sendPacket(BukkitAdaptors.adapt(player), bundlePacket);
        } catch (ReflectiveOperationException e) {
            BukkitCraftEngine.instance().logger().warn("Failed to send totem animation");
        }
    }

    public static Set<Player> getTrackedBy(Player player) {
        return VersionHelper.isOrAbove1_20_2() ? player.getTrackedBy() : player.getTrackedPlayers();
    }
}
