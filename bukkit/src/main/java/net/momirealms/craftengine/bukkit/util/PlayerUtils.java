package net.momirealms.craftengine.bukkit.util;

import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.sound.Sounds;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    public static void giveItem(Player player, int amount, Item<ItemStack> original) {
        int amountToGive = amount;
        int maxStack = original.maxStackSize();
        while (amountToGive > 0) {
            int perStackSize = Math.min(maxStack, amountToGive);
            amountToGive -= perStackSize;
            PlayerUtils.giveItem(player, original, original.copyWithCount(perStackSize));
        }
    }

    public static void giveItem(Player player, Item<ItemStack> original, Item<ItemStack> item) {
        if (player == null) return;
        Object serverPlayer = player.serverPlayer();
        Object inventory = FastNMS.INSTANCE.method$Player$getInventory(serverPlayer);
        boolean flag = FastNMS.INSTANCE.method$Inventory$add(inventory, item.getLiteralObject());
        if (flag && item.isEmpty()) {
            Object droppedItem = FastNMS.INSTANCE.method$ServerPlayer$drop(serverPlayer, original.copyWithCount(1).getLiteralObject(), false, false, false, null);
            if (droppedItem != null) {
                FastNMS.INSTANCE.method$ItemEntity$makeFakeItem(droppedItem);
            }
            player.world().playSound(player.position(), Sounds.ENTITY_ITEM_PICKUP, 0.2F, ((RandomUtils.generateRandomFloat(0, 1) - RandomUtils.generateRandomFloat(0, 1)) * 0.7F + 1.0F) * 2.0F, SoundSource.PLAYER);
            FastNMS.INSTANCE.method$AbstractContainerMenu$broadcastChanges(FastNMS.INSTANCE.field$Player$containerMenu(serverPlayer));
        } else {
            Object droppedItem = FastNMS.INSTANCE.method$ServerPlayer$drop(serverPlayer, item.getLiteralObject(), false, false, !VersionHelper.isOrAbove1_21_5(), null);
            if (droppedItem != null) {
                FastNMS.INSTANCE.method$ItemEntity$setNoPickUpDelay(droppedItem);
                FastNMS.INSTANCE.method$ItemEntity$setTarget(droppedItem, player.uuid());
            }
        }
    }

    public static void sendTotemAnimation(Player player, Item<?> totem, @Nullable SoundData sound, boolean silent) {
        List<Object> packets = new ArrayList<>();
        try {
            Object totemItem = totem.getLiteralObject();
            Item<?> previousMainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean isMainHandTotem;
            if (VersionHelper.isOrAbove1_21_2()) {
                isMainHandTotem = previousMainHandItem.hasComponent(DataComponentTypes.DEATH_PROTECTION);
            } else {
                isMainHandTotem = previousMainHandItem.id().equals(ItemKeys.TOTEM_OF_UNDYING);
            }
            Object previousOffHandItem = player.getItemInHand(InteractionHand.OFF_HAND).getLiteralObject();
            if (isMainHandTotem) {
                packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                        player.entityId(), List.of(Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, BukkitItemManager.instance().uniqueEmptyItem().item().getLiteralObject()))
                ));
            }
            packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                    player.entityId(), List.of(Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, totemItem))
            ));
            packets.add(NetworkReflections.constructor$ClientboundEntityEventPacket.newInstance(player.serverPlayer(), (byte) 35));
            if (isMainHandTotem) {
                packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                        player.entityId(), List.of(Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, previousMainHandItem.getLiteralObject()))
                ));
            }
            packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                    player.entityId(), List.of(Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, previousOffHandItem))
            ));
            if (sound != null || silent) {
                packets.add(NetworkReflections.constructor$ClientboundStopSoundPacket.newInstance(
                        FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item.totem.use"),
                        CoreReflections.instance$SoundSource$PLAYERS
                ));
            }
            if (sound != null) {
                packets.add(FastNMS.INSTANCE.constructor$ClientboundSoundPacket(
                        FastNMS.INSTANCE.method$Holder$direct(FastNMS.INSTANCE.constructor$SoundEvent(KeyUtils.toResourceLocation(sound.id()), Optional.empty())),
                        CoreReflections.instance$SoundSource$PLAYERS,
                        player.x(), player.y(), player.z(), sound.volume().get(), sound.pitch().get(),
                        RandomUtils.generateRandomLong()
                ));
            }
            player.sendPackets(packets, false);
        } catch (ReflectiveOperationException e) {
            BukkitCraftEngine.instance().logger().warn("Failed to send totem animation");
        }
    }
}
