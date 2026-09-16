package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitProjectileManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.network.handler.*;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.projectile.ProjectileDisplay;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;

import java.util.Arrays;
import java.util.UUID;

public final class AddEntityListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new AddEntityListener();
    private final EntityTypeHandler[] handlers;

    private AddEntityListener() {
        this.handlers = new EntityTypeHandler[RegistryUtils.currentEntityTypeRegistrySize()];
        Arrays.fill(this.handlers, EntityTypeHandler.DoNothing.INSTANCE);
        // 性能模式由 NMS 监听器统一替换，字节层只为下列实体类型解析数据
        boolean byteBufEntityData = !Config.nettyPerformanceModeEntity();
        this.handlers[EntityTypesProxy.ITEM$registryId] = simpleAddEntityHandler(ItemPacketHandler.INSTANCE);
        this.handlers[EntityTypesProxy.FIREBALL$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.EYE_OF_ENDER$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.FIREWORK_ROCKET$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.SMALL_FIREBALL$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.EGG$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.ENDER_PEARL$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.EXPERIENCE_BOTTLE$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.SNOWBALL$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.POTION$registryId] = createOptionalCustomProjectileEntityHandler(byteBufEntityData);
        this.handlers[EntityTypesProxy.TRIDENT$registryId] = createOptionalCustomProjectileEntityHandler(false);
        this.handlers[EntityTypesProxy.ARROW$registryId] = createOptionalCustomProjectileEntityHandler(false);
        this.handlers[EntityTypesProxy.SPECTRAL_ARROW$registryId] = createOptionalCustomProjectileEntityHandler(false);
        if (VersionHelper.isOrAbove1_21) {
            this.handlers[EntityTypesProxy.WIND_CHARGE$registryId] = createOptionalCustomProjectileEntityHandler(false);
        }
        int[] minecartTypes = {
                EntityTypesProxy.CHEST_MINECART$registryId, EntityTypesProxy.COMMAND_BLOCK_MINECART$registryId, EntityTypesProxy.FURNACE_MINECART$registryId,
                EntityTypesProxy.HOPPER_MINECART$registryId, EntityTypesProxy.MINECART$registryId, EntityTypesProxy.SPAWNER_MINECART$registryId,
                EntityTypesProxy.TNT_MINECART$registryId};
        if (VersionHelper.isOrAbove1_21_5) {
            if (byteBufEntityData) {
                for (int type : minecartTypes) {
                    this.handlers[type] = simpleAddEntityHandler(EntityDataPacketHandler.INSTANCE);
                }
            }
        } else {
            // 旧版矿车的展示方块是 INT 数据，NMS 监听器也识别不出来，两种模式下都得在字节层处理
            for (int type : minecartTypes) {
                this.handlers[type] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            }
        }
        if (byteBufEntityData) {
            for (int type : new int[]{
                    EntityTypesProxy.BLOCK_DISPLAY$registryId, EntityTypesProxy.TEXT_DISPLAY$registryId, EntityTypesProxy.ITEM_FRAME$registryId,
                    EntityTypesProxy.GLOW_ITEM_FRAME$registryId, EntityTypesProxy.ENDERMAN$registryId, EntityTypesProxy.ARMOR_STAND$registryId}) {
                this.handlers[type] = simpleAddEntityHandler(EntityDataPacketHandler.INSTANCE);
            }
            if (VersionHelper.isOrAbove1_20_3) {
                this.handlers[EntityTypesProxy.TNT$registryId] = simpleAddEntityHandler(EntityDataPacketHandler.INSTANCE);
            }
        }
        if (VersionHelper.isOrAbove1_20_5) {
            this.handlers[EntityTypesProxy.OMINOUS_ITEM_SPAWNER$registryId] = simpleAddEntityHandler(ItemPacketHandler.INSTANCE);
        }
        this.handlers[EntityTypesProxy.FALLING_BLOCK$registryId] = (user, event) -> {
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
            int remapped = BukkitNetworkManager.instance().remapBlockState(data, user.clientCustomBlockEnabled());
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
        this.handlers[EntityTypesProxy.ITEM_DISPLAY$registryId] = (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(id);
            if (furniture != null) {
                EntityPacketHandler previous = serverPlayer.entityViews().get(id);
                // 补发生成包不能替换仍在追踪的处理器，否则会丢失行为快照并重复登记光照。
                if (!(previous instanceof FurniturePacketHandler handler) || handler.furniture != furniture) {
                    FurniturePacketHandler furniturePacketHandler = new FurniturePacketHandler(furniture);
                    serverPlayer.entityViews().put(id, furniturePacketHandler);
                    if (Config.enableEntityCulling()) {
                        serverPlayer.addTrackedEntity(id, furniturePacketHandler);
                    }
                    furniturePacketHandler.synchronize(serverPlayer);
                } else {
                    // 首包可能早于 onLoad 完成，补包时复用处理器并同步已发布的快照。
                    handler.synchronize(serverPlayer);
                }
                if (Config.hideBaseEntity() && !furniture.hasExternalModel()) {
                    event.setCancelled(true);
                }
            } else if (byteBufEntityData) {
                user.entityViews().putIfAbsent(id, EntityDataPacketHandler.INSTANCE);
            }
        };
        this.handlers[EntityTypesProxy.INTERACTION$registryId] = (user, event) -> {
            if (BukkitFurnitureManager.NMS_COLLISION_ENTITY_TYPE != EntityTypesProxy.INTERACTION) return;
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            // Cancel collider entity packet
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByColliderEntityId(id);
            if (furniture != null) {
                event.setCancelled(true);
                user.entityViews().put(id, FurnitureCollisionPacketHandler.INSTANCE);
            }
        };
        this.handlers[EntityTypesProxy.OAK_BOAT$registryId] = (user, event) -> {
            if (BukkitFurnitureManager.NMS_COLLISION_ENTITY_TYPE != EntityTypesProxy.OAK_BOAT) return;
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            // Cancel collider entity packet
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByColliderEntityId(id);
            if (furniture != null) {
                event.setCancelled(true);
                user.entityViews().put(id, FurnitureCollisionPacketHandler.INSTANCE);
            }
        };
        if (Config.enableEquipmentLod()) {
            for (String name : new String[]{"player", "armor_stand", "mannequin", "zombie", "zombie_villager", "husk", "drowned",
                    "skeleton", "stray", "wither_skeleton", "bogged", "parched", "piglin", "piglin_brute", "zombified_piglin",
                    "giant", "wolf", "horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "llama",
                    "trader_llama", "pig", "strider", "camel", "camel_husk", "happy_ghast", "nautilus", "zombie_nautilus"}) {
                Object type = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ENTITY_TYPE, KeyUtils.toIdentifier(Key.MINECRAFT_NAMESPACE, name));
                if (type == null) continue;
                int id = RegistryProxy.INSTANCE.getId(BuiltInRegistriesProxy.ENTITY_TYPE, type);
                EntityPacketHandler handler = byteBufEntityData && "armor_stand".equals(name) ? ArmorStandPacketHandler.INSTANCE : EquipmentEntityPacketHandler.INSTANCE;
                this.handlers[id] = simpleAddEntityHandler(handler);
            }
        }
    }

    private static EntityTypeHandler simpleAddEntityHandler(EntityPacketHandler handler) {
        return (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            int entityId = buf.readVarInt();
            user.entityViews().put(entityId, handler);
            handler.handleAddEntity(user, event, entityId, buf);
        };
    }

    private static EntityTypeHandler createOptionalCustomProjectileEntityHandler(boolean genericEntityData) {
        return (user, event) -> {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            BukkitProjectileManager.instance().projectileByEntityId(id).ifPresentOrElse(customProjectile -> {
                ProjectileDisplay display = customProjectile.metadata().display();
                if (display != null) {
                    ProjectilePacketHandler handler = new ProjectilePacketHandler(customProjectile, display, id);
                    handler.convertAddCustomProjectilePacket(buf, event, user);
                    user.entityViews().put(id, handler);
                } else if (genericEntityData) {
                    user.entityViews().put(id, EntityDataPacketHandler.INSTANCE);
                }
            }, () -> {
                if (genericEntityData) {
                    user.entityViews().put(id, EntityDataPacketHandler.INSTANCE);
                }
            });
        };
    }

    public interface EntityTypeHandler {

        void handle(Player user, ByteBufPacketEvent event);

        class DoNothing implements EntityTypeHandler {
            public static final DoNothing INSTANCE = new DoNothing();

            @Override
            public void handle(Player user, ByteBufPacketEvent event) {
            }
        }
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!(user instanceof Player player)) return;
        FriendlyByteBuf buf = event.getBuffer();
        buf.readVarInt();
        buf.readUUID();
        int type = buf.readVarInt();
        this.handlers[type].handle(player, event);
    }
}
