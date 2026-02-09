package net.momirealms.craftengine.bukkit.util;

import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
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
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSoundPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.ItemEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.InventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
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
        Object inventory = PlayerProxy.INSTANCE.getInventory(serverPlayer);
        boolean flag = InventoryProxy.INSTANCE.add(inventory, item.getLiteralObject());
        if (flag && item.isEmpty()) {
            Object droppedItem;
            if (VersionHelper.isOrAbove1_21_4()) {
                droppedItem = ServerPlayerProxy.INSTANCE.drop(serverPlayer, original.copyWithCount(1).getLiteralObject(), false, false, false, null);
            } else {
                droppedItem = ServerPlayerProxy.INSTANCE.drop(serverPlayer, original.copyWithCount(1).getLiteralObject(), false, false, false);
            }
            if (droppedItem != null) {
                ItemEntityProxy.INSTANCE.makeFakeItem(droppedItem);
            }
            player.world().playSound(player.position(), Sounds.ENTITY_ITEM_PICKUP, 0.2F, ((RandomUtils.generateRandomFloat(0, 1) - RandomUtils.generateRandomFloat(0, 1)) * 0.7F + 1.0F) * 2.0F, SoundSource.PLAYER);
            AbstractContainerMenuProxy.INSTANCE.broadcastChanges(FastNMS.INSTANCE.field$Player$containerMenu(serverPlayer));
        } else {
            Object droppedItem;
            if (VersionHelper.isOrAbove1_21_4()) {
                droppedItem = ServerPlayerProxy.INSTANCE.drop(serverPlayer, item.getLiteralObject(), false, false, !VersionHelper.isOrAbove1_21_5(), null);
            } else {
                droppedItem = ServerPlayerProxy.INSTANCE.drop(serverPlayer, item.getLiteralObject(), false, false, true);
            }
            if (droppedItem != null) {
                ItemEntityProxy.INSTANCE.setNoPickUpDelay(droppedItem);
                ItemEntityProxy.INSTANCE.setTarget$1(droppedItem, player.uuid());
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
                        player.entityId(), List.of(Pair.of(EquipmentSlotProxy.MAINHAND, BukkitItemManager.instance().uniqueEmptyItem().item().getLiteralObject()))
                ));
            }
            packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                    player.entityId(), List.of(Pair.of(EquipmentSlotProxy.OFFHAND, totemItem))
            ));
            packets.add(NetworkReflections.constructor$ClientboundEntityEventPacket.newInstance(player.serverPlayer(), (byte) 35));
            if (isMainHandTotem) {
                packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                        player.entityId(), List.of(Pair.of(EquipmentSlotProxy.MAINHAND, previousMainHandItem.getLiteralObject()))
                ));
            }
            packets.add(NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(
                    player.entityId(), List.of(Pair.of(EquipmentSlotProxy.OFFHAND, previousOffHandItem))
            ));
            if (sound != null || silent) {
                packets.add(NetworkReflections.constructor$ClientboundStopSoundPacket.newInstance(
                        FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item.totem.use"),
                        SoundSourceProxy.PLAYERS
                ));
            }
            if (sound != null) {
                packets.add(ClientboundSoundPacketProxy.INSTANCE.newInstance(
                        HolderProxy.INSTANCE.direct(FastNMS.INSTANCE.constructor$SoundEvent(KeyUtils.toIdentifier(sound.id()), Optional.empty())),
                        SoundSourceProxy.PLAYERS,
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
