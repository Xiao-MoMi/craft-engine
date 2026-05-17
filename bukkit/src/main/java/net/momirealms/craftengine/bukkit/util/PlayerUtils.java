package net.momirealms.craftengine.bukkit.util;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.item.ItemEntityData;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.sound.Sounds;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.ItemEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.InventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    public static void giveItem(@NotNull Player player, int amount, Item original, boolean spawnEntity) {
        int amountToGive = amount;
        int maxStack = original.maxStackSize();
        while (amountToGive > 0) {
            int perStackSize = Math.min(maxStack, amountToGive);
            amountToGive -= perStackSize;
            PlayerUtils.giveItem(player, original, original.copyWithCount(perStackSize), spawnEntity);
        }
    }

    public static void giveItem(@NotNull Player player, Item original, Item item, boolean spawnFakeEntity) {
        Object serverPlayer = player.serverPlayer();
        Object inventory = PlayerProxy.INSTANCE.getInventory(serverPlayer);
        boolean flag = InventoryProxy.INSTANCE.add(inventory, item.minecraftItem());
        if (flag && item.isEmpty()) {
            if (spawnFakeEntity) {
                double pitchRad = player.xRot() * (Math.PI / 180F);
                double yawRad = player.yRot() * (Math.PI / 180F);

                double sinPitch = Math.sin(pitchRad);
                double cosPitch = Math.cos(pitchRad);
                double sinYaw = Math.sin(yawRad);
                double cosYaw = Math.cos(yawRad);

                float randomAngle = RandomUtils.generateRandomFloat() * ((float) Math.PI * 2F);
                float spreadIntensity = 0.02F * RandomUtils.generateRandomFloat();

                int entityId = EntityProxy.ENTITY_COUNTER.incrementAndGet();

                double velX = (-sinYaw * cosPitch * 0.3F) + Math.cos(randomAngle) * (double) spreadIntensity;
                double velY = -sinPitch * 0.3F + 0.1F + (RandomUtils.generateRandomFloat() - RandomUtils.generateRandomFloat()) * 0.1F;
                double velZ = (cosYaw * cosPitch * 0.3F) + Math.sin(randomAngle) * (double) spreadIntensity;

                Object addEntityPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                        entityId,
                        UUID.randomUUID(),
                        player.x(),
                        EntityProxy.INSTANCE.getEyeY(player.serverPlayer()) - 0.3,
                        player.z(),
                        player.xRot(),
                        player.yRot(),
                        EntityTypeProxy.ITEM,
                        0,
                        Vec3Proxy.INSTANCE.newInstance(velX, velY, velZ),
                        0
                );

                Object itemMetaPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(
                        entityId,
                        List.of(ItemEntityData.Item.createEntityData(original.copyWithCount(1).minecraftItem()))
                );

                player.sendPackets(List.of(addEntityPacket, itemMetaPacket), false);
                player.world().playSound(player.position(), Sounds.ENTITY_ITEM_PICKUP, 0.2F, ((RandomUtils.generateRandomFloat() - RandomUtils.generateRandomFloat()) * 0.7F + 1.0F) * 2.0F, SoundSource.PLAYER);
                BukkitCraftEngine.instance().scheduler().platform().runDelayed(() -> {
                    player.sendPacket(ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), k -> k.add(entityId))), false);
                }, null, (org.bukkit.entity.Player) player.platformPlayer());
            }
            AbstractContainerMenuProxy.INSTANCE.broadcastChanges(PlayerProxy.INSTANCE.getContainerMenu(serverPlayer));
        } else {
            Object droppedItem;
            if (VersionHelper.isOrAbove1_21_4) {
                droppedItem = ServerPlayerProxy.INSTANCE.drop(serverPlayer, item.minecraftItem(), false, false, !VersionHelper.isOrAbove1_21_5, null);
            } else if (VersionHelper.isOrAbove1_20_3) {
                droppedItem = ServerPlayerProxy.INSTANCE.drop$1(serverPlayer, item.minecraftItem(), false, false, true);
            } else {
                droppedItem = PlayerProxy.INSTANCE.drop(serverPlayer, item.minecraftItem(), false, false, true);
            }
            if (droppedItem != null) {
                ItemEntityProxy.INSTANCE.setNoPickUpDelay(droppedItem);
                ItemEntityProxy.INSTANCE.setTarget$1(droppedItem, player.uuid());
            }
        }
    }

    public static void sendTotemAnimation(Player player, Item totem, @Nullable SoundData sound, boolean silent) {
        List<Object> packets = new ArrayList<>();
        Object totemItem = totem.minecraftItem();
        Item previousMainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isMainHandTotem;
        if (VersionHelper.isOrAbove1_21_2) {
            isMainHandTotem = previousMainHandItem.hasComponent(DataComponentTypes.DEATH_PROTECTION);
        } else {
            isMainHandTotem = previousMainHandItem.id().equals(ItemKeys.TOTEM_OF_UNDYING);
        }
        Object previousOffHandItem = player.getItemInHand(InteractionHand.OFF_HAND).minecraftItem();
        if (isMainHandTotem) {
            packets.add(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(
                    player.entityId(), List.of(Pair.of(EquipmentSlotProxy.MAINHAND, ItemStackProxy.EMPTY))
            ));
        }
        packets.add(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(
                player.entityId(), List.of(Pair.of(EquipmentSlotProxy.OFFHAND, totemItem))
        ));
        packets.add(ClientboundEntityEventPacketProxy.INSTANCE.newInstance(player.serverPlayer(), (byte) 35));
        if (isMainHandTotem) {
            packets.add(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(
                    player.entityId(), List.of(Pair.of(EquipmentSlotProxy.MAINHAND, previousMainHandItem.minecraftItem()))
            ));
        }
        packets.add(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(
                player.entityId(), List.of(Pair.of(EquipmentSlotProxy.OFFHAND, previousOffHandItem))
        ));
        if (sound != null || silent) {
            packets.add(ClientboundStopSoundPacketProxy.INSTANCE.newInstance(
                    IdentifierProxy.INSTANCE.newInstance("minecraft", "item.totem.use"),
                    SoundSourceProxy.PLAYERS
            ));
        }
        if (sound != null) {
            packets.add(ClientboundSoundPacketProxy.INSTANCE.newInstance(
                    HolderProxy.INSTANCE.direct(SoundEventProxy.INSTANCE.create(KeyUtils.toIdentifier(sound.id()), Optional.empty())),
                    SoundSourceProxy.PLAYERS,
                    player.x(), player.y(), player.z(), sound.volume().get(), sound.pitch().get(),
                    RandomUtils.generateRandomLong()
            ));
        }
        player.sendPackets(packets, false);
    }
}
