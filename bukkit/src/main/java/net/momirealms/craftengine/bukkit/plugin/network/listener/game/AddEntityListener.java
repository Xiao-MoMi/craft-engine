package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitProjectileManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.network.handler.*;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;

import java.util.Arrays;
import java.util.UUID;

public final class AddEntityListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new AddEntityListener();
    private final EntityTypeHandler[] handlers;

    private AddEntityListener() {
        this.handlers = new EntityTypeHandler[RegistryUtils.currentEntityTypeRegistrySize()];
        Arrays.fill(this.handlers, EntityTypeHandler.DoNothing.INSTANCE);
        this.handlers[EntityTypeProxy.BLOCK_DISPLAY$registryId] = simpleAddEntityHandler(BlockDisplayPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.TEXT_DISPLAY$registryId] = simpleAddEntityHandler(TextDisplayPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.ARMOR_STAND$registryId] = simpleAddEntityHandler(ArmorStandPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.ITEM$registryId] = simpleAddEntityHandler(ItemPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.ITEM_FRAME$registryId] = simpleAddEntityHandler(ItemFramePacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.GLOW_ITEM_FRAME$registryId] = simpleAddEntityHandler(ItemFramePacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.ENDERMAN$registryId] = simpleAddEntityHandler(EndermanPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.CHEST_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.COMMAND_BLOCK_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.FURNACE_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.HOPPER_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.SPAWNER_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.TNT_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
        this.handlers[EntityTypeProxy.FIREBALL$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.EYE_OF_ENDER$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.FIREWORK_ROCKET$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.SMALL_FIREBALL$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.EGG$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.ENDER_PEARL$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.EXPERIENCE_BOTTLE$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.SNOWBALL$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.POTION$registryId] = createOptionalCustomProjectileEntityHandler(true);
        this.handlers[EntityTypeProxy.TRIDENT$registryId] = createOptionalCustomProjectileEntityHandler(false);
        this.handlers[EntityTypeProxy.ARROW$registryId] = createOptionalCustomProjectileEntityHandler(false);
        this.handlers[EntityTypeProxy.SPECTRAL_ARROW$registryId] = createOptionalCustomProjectileEntityHandler(false);
        if (VersionHelper.isOrAbove1_20_3) {
            this.handlers[EntityTypeProxy.TNT$registryId] = simpleAddEntityHandler(PrimedTNTPacketHandler.INSTANCE);
        }
        if (VersionHelper.isOrAbove1_20_5) {
            this.handlers[EntityTypeProxy.OMINOUS_ITEM_SPAWNER$registryId] = simpleAddEntityHandler(ItemPacketHandler.INSTANCE);
        }
        this.handlers[EntityTypeProxy.FALLING_BLOCK$registryId] = (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            UUID uuid = buf.readUUID();
            int type = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            Vec3d movement = VersionHelper.isOrAbove1_21_9 ? buf.readLpVec3() : null;
            byte xRot = buf.readByte();
            byte yRot = buf.readByte();
            byte yHeadRot = buf.readByte();
            int data = buf.readVarInt();
            // Falling blocks
            int remapped = BukkitNetworkManager.instance().remapBlockState(data, user.clientModEnabled());
            if (remapped != data) {
                int xa = VersionHelper.isOrAbove1_21_9 ? -1 : buf.readShort();
                int ya = VersionHelper.isOrAbove1_21_9 ? -1 : buf.readShort();
                int za = VersionHelper.isOrAbove1_21_9 ? -1 : buf.readShort();
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(id);
                buf.writeUUID(uuid);
                buf.writeVarInt(type);
                buf.writeDouble(x);
                buf.writeDouble(y);
                buf.writeDouble(z);
                if (VersionHelper.isOrAbove1_21_9) buf.writeLpVec3(movement);
                buf.writeByte(xRot);
                buf.writeByte(yRot);
                buf.writeByte(yHeadRot);
                buf.writeVarInt(remapped);
                if (!VersionHelper.isOrAbove1_21_9) buf.writeShort(xa);
                if (!VersionHelper.isOrAbove1_21_9) buf.writeShort(ya);
                if (!VersionHelper.isOrAbove1_21_9) buf.writeShort(za);
            }
        };
        this.handlers[EntityTypeProxy.ITEM_DISPLAY$registryId] = (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(id);
            if (furniture != null) {
                FurniturePacketHandler furniturePacketHandler = new FurniturePacketHandler(furniture);
                EntityPacketHandler previous = serverPlayer.entityPacketHandlers().put(id, furniturePacketHandler);
                if (Config.enableEntityCulling()) {
                    serverPlayer.addTrackedEntity(id, furniture);
                    furniture.controller.onAsyncPlayerTrack(serverPlayer, furniturePacketHandler.snapshotState);
                } else {
                    // 修复addEntityToWorld，包比事件先发的问题 (WE)
                    if (previous == null || previous instanceof ItemDisplayPacketHandler) {
                        furniture.show(serverPlayer);
                        furniture.controller.onAsyncPlayerTrack(serverPlayer, furniturePacketHandler.snapshotState);
                    }
                }
                if (Config.hideBaseEntity() && !furniture.hasExternalModel()) {
                    event.setCancelled(true);
                }
            } else {
                user.entityPacketHandlers().putIfAbsent(id, ItemDisplayPacketHandler.INSTANCE);
            }
        };
        this.handlers[EntityTypeProxy.INTERACTION$registryId] = (user, event) -> {
            if (BukkitFurnitureManager.NMS_COLLISION_ENTITY_TYPE != EntityTypeProxy.INTERACTION) return;
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            // Cancel collider entity packet
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByColliderEntityId(id);
            if (furniture != null) {
                event.setCancelled(true);
                user.entityPacketHandlers().put(id, FurnitureCollisionPacketHandler.INSTANCE);
            }
        };
        this.handlers[EntityTypeProxy.OAK_BOAT$registryId] = (user, event) -> {
            if (BukkitFurnitureManager.NMS_COLLISION_ENTITY_TYPE != EntityTypeProxy.OAK_BOAT) return;
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            // Cancel collider entity packet
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByColliderEntityId(id);
            if (furniture != null) {
                event.setCancelled(true);
                user.entityPacketHandlers().put(id, FurnitureCollisionPacketHandler.INSTANCE);
            }
        };
    }

    private static EntityTypeHandler simpleAddEntityHandler(EntityPacketHandler handler) {
        return (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            user.entityPacketHandlers().put(buf.readVarInt(), handler);
        };
    }

    private static EntityTypeHandler createOptionalCustomProjectileEntityHandler(boolean fallback) {
        return (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            BukkitProjectileManager.instance().projectileByEntityId(id).ifPresentOrElse(customProjectile -> {
                ProjectilePacketHandler handler = new ProjectilePacketHandler(customProjectile, id);
                handler.convertAddCustomProjectilePacket(buf, event, user);
                user.entityPacketHandlers().put(id, handler);
            }, () -> {
                if (fallback) {
                    user.entityPacketHandlers().put(id, CommonItemPacketHandler.INSTANCE);
                }
            });
        };
    }

    public interface EntityTypeHandler {

        void handle(NetWorkUser user, ByteBufPacketEvent event);

        class DoNothing implements EntityTypeHandler {
            public static final DoNothing INSTANCE = new DoNothing();

            @Override
            public void handle(NetWorkUser user, ByteBufPacketEvent event) {
            }
        }
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        buf.readVarInt();
        buf.readUUID();
        int type = buf.readVarInt();
        this.handlers[type].handle(user, event);
    }
}
