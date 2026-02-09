package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundTeleportEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PoseProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PositionMoveRotationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.vehicle.DismountHelperProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;
import java.util.function.Consumer;

public final class EntityUtils {

    private EntityUtils() {}

    public static Object createUpdatePosPacket(int entityId, double x, double y, double z, float yRot, float xRot, boolean onGround) {
        if (VersionHelper.isOrAbove1_21_2()) {
            Object position = Vec3Proxy.INSTANCE.newInstance(x, y, z);
            Object values = PositionMoveRotationProxy.INSTANCE.newInstance(position, Vec3Proxy.ZERO, yRot, xRot);
            return ClientboundEntityPositionSyncPacketProxy.INSTANCE.newInstance(entityId, values, onGround);
        } else {
            Object packet = ClientboundTeleportEntityPacketProxy.UNSAFE_CONSTRUCTOR.newInstance();
            ClientboundTeleportEntityPacketProxy.INSTANCE.setId(packet, entityId);
            ClientboundTeleportEntityPacketProxy.INSTANCE.setX(packet, x);
            ClientboundTeleportEntityPacketProxy.INSTANCE.setY(packet, y);
            ClientboundTeleportEntityPacketProxy.INSTANCE.setZ(packet, z);
            ClientboundTeleportEntityPacketProxy.INSTANCE.setYRot(packet, MiscUtils.packDegrees(yRot));
            ClientboundTeleportEntityPacketProxy.INSTANCE.setXRot(packet, MiscUtils.packDegrees(xRot));
            ClientboundTeleportEntityPacketProxy.INSTANCE.setOnGround(packet, onGround);
            return packet;
        }
    }

    public static Vec3d getPassengerRidingPosition(Entity vehicle, Entity passenger) {
        Object nmsVehicle = CraftEntityProxy.INSTANCE.getEntity(vehicle);
        Object nmsPassenger = CraftEntityProxy.INSTANCE.getEntity(passenger);
        if (VersionHelper.isOrAbove1_20_5()) {
            Vec3d passengerRidingPosition = LocationUtils.fromVec(EntityProxy.INSTANCE.getPassengerRidingPosition(nmsVehicle, nmsPassenger));
            Vec3d vehicleAttachmentPoint = LocationUtils.fromVec(EntityProxy.INSTANCE.getVehicleAttachmentPoint(nmsVehicle, nmsPassenger));
            return passengerRidingPosition.subtract(vehicleAttachmentPoint);
        } else if (VersionHelper.isOrAbove1_20_2()) {
            Vec3d passengerRidingPosition = LocationUtils.fromVec(EntityProxy.INSTANCE.getPassengerRidingPosition(nmsVehicle, nmsPassenger));
            return passengerRidingPosition.add(0, EntityProxy.INSTANCE.getMyRidingOffset(nmsVehicle, nmsPassenger), 0);
        } else {
            Vec3d pos = LocationUtils.fromVec(EntityProxy.INSTANCE.getPosition(nmsVehicle));
            return pos.add(0, EntityProxy.INSTANCE.getPassengersRidingOffset(nmsVehicle) + EntityProxy.INSTANCE.getMyRidingOffset(nmsPassenger), 0);
        }
    }

    public static BlockPos getOnPos(Player player) {
        Object serverPlayer = FastNMS.INSTANCE.method$CraftPlayer$getHandle(player);
        Object blockPos = EntityProxy.INSTANCE.getOnPos(serverPlayer);
        return LocationUtils.fromBlockPos(blockPos);
    }

    public static Entity spawnEntity(World world, Location loc, EntityType type, Consumer<Entity> function) {
        if (VersionHelper.isOrAbove1_20_2()) {
            return world.spawnEntity(loc, type, CreatureSpawnEvent.SpawnReason.CUSTOM, function);
        } else {
            return LegacyEntityUtils.spawnEntity(world, loc, type, function);
        }
    }

    public static Key getEntityType(Entity entity) {
        Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(entity);
        Object entityType = EntityProxy.INSTANCE.getType(nmsEntity);
        Object id = FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ENTITY_TYPE, entityType);
        return KeyUtils.identifierToKey(id);
    }

    public static void safeDismount(Player player, Location location) {
        double boundBoxWidth = player.getBoundingBox().getWidthX();
        for (int i = 0; i < 8; i++) {
            Vec3d direction = getHorizontalDirection(i * 0.25, boundBoxWidth, player.getYaw());
            double x = location.getX() + direction.x;
            double y = location.getY();
            double z = location.getZ() + direction.z;
            Object serverLevel = BukkitAdaptors.adapt(player.getWorld()).serverWorld();
            Object serverPlayer = FastNMS.INSTANCE.method$CraftPlayer$getHandle(player);
            for (Object pose : List.of(PoseProxy.STANDING, PoseProxy.CROUCHING, PoseProxy.SWIMMING)) {
                BlockPos pos = new BlockPos(MiscUtils.floor(x), MiscUtils.floor(y), MiscUtils.floor(z));
                double floorHeight = BlockGetterProxy.INSTANCE.getBlockFloorHeight(serverLevel, LocationUtils.toBlockPos(pos));
                if (pos.y() + floorHeight > y + 0.75 || !isBlockFloorValid(floorHeight)) {
                    floorHeight = BlockGetterProxy.INSTANCE.getBlockFloorHeight(serverLevel, LocationUtils.toBlockPos(pos.below()));
                    if (pos.y() + floorHeight - 1 < y - 0.75 || !isBlockFloorValid(floorHeight)) {
                        continue;
                    }
                    floorHeight -= 1;
                }
                Object aabb = LivingEntityProxy.INSTANCE.getLocalBoundsForPose(serverPlayer, pose);
                Object vec3 = Vec3Proxy.INSTANCE.newInstance(x, pos.y() + floorHeight, z);
                Object newAABB = FastNMS.INSTANCE.method$AABB$move(aabb, vec3);
                boolean canDismount = DismountHelperProxy.INSTANCE.canDismountTo(serverLevel, serverPlayer, newAABB);
                if (!canDismount) {
                    continue;
                }
                if (!FastNMS.INSTANCE.checkEntityCollision(serverLevel, List.of(newAABB), o -> true)) {
                    continue;
                }
                if (VersionHelper.isFolia()) {
                    player.teleportAsync(new Location(player.getWorld(), x, pos.y() + floorHeight, z, player.getYaw(), player.getPitch()));
                } else {
                    player.teleport(new Location(player.getWorld(), x, pos.y() + floorHeight, z, player.getYaw(), player.getPitch()));
                }
                if (pose == PoseProxy.STANDING) {
                    player.setPose(Pose.STANDING);
                } else if (pose == PoseProxy.CROUCHING) {
                    player.setPose(Pose.SNEAKING);
                } else if (pose == PoseProxy.SWIMMING) {
                    player.setPose(Pose.SWIMMING);
                }
            }
        }
    }

    private static Vec3d getHorizontalDirection(double hitboxWidth, double passengerWidth, float passengerYaw) {
        double d2 = (hitboxWidth + passengerWidth + (double) 1.0E-5F) / (double) 2.0F;
        float f1 = -MiscUtils.sin(passengerYaw * ((float) Math.PI / 180F));
        float f2 = MiscUtils.cos(passengerYaw * ((float) Math.PI / 180F));
        float f3 = Math.max(Math.abs(f1), Math.abs(f2));
        return new Vec3d((double)f1 * d2 / (double) f3, 0.0F, (double) f2 * d2 / (double)f3);
    }

    private static boolean isBlockFloorValid(double height) {
        return !Double.isInfinite(height) && height < (double) 1.0F;
    }
}