package net.momirealms.craftengine.bukkit.util;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.entity.BukkitItemEntity;
import net.momirealms.craftengine.bukkit.entity.BukkitLivingEntity;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.core.entity.data.EntityData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundTeleportEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ChunkMapProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerPlayerConnectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PoseProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PositionMoveRotationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.ItemEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.vehicle.DismountHelperProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.CollisionGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EntityUtils {
    public static final AtomicInteger ENTITY_COUNTER = VersionHelper.isOrAbove26_2 ? ServerLevelProxy.INSTANCE.getEntityCounter() : EntityProxy.INSTANCE.getEntityCounter();
    public static final Map<Class<?>, Function<Object, net.momirealms.craftengine.core.entity.Entity>> ENTITY_ADAPTORS = MiscUtils.init(new Object2ObjectOpenHashMap<>(), m -> {
        m.put(ServerPlayerProxy.CLASS, e -> BukkitNetworkManager.instance().getOnlineUser(EntityProxy.INSTANCE.getUUID(e)));
        m.put(PlayerProxy.CLASS, e -> BukkitNetworkManager.instance().getOnlineUser(EntityProxy.INSTANCE.getUUID(e)));
        m.put(LivingEntityProxy.CLASS, BukkitLivingEntity::new);
        m.put(ItemEntityProxy.CLASS, BukkitItemEntity::new);
        m.put(EntityProxy.CLASS, BukkitEntity::new);
    });
    private static final Map<Class<?>, Function<Object, net.momirealms.craftengine.core.entity.Entity>> CACHED_ADAPTORS = new ConcurrentHashMap<>();

    private EntityUtils() {}

    public static Object createUpdatePosPacket(int entityId, double x, double y, double z, float yRot, float xRot, boolean onGround) {
        if (VersionHelper.isOrAbove1_21_2) {
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

    public static Vec3d getPassengerRidingPosition(Object nmsVehicle, Object nmsPassenger) {
        if (VersionHelper.isOrAbove1_20_5) {
            Vec3d passengerRidingPosition = LocationUtils.fromVec(EntityProxy.INSTANCE.getPassengerRidingPosition(nmsVehicle, nmsPassenger));
            Vec3d vehicleAttachmentPoint = LocationUtils.fromVec(EntityProxy.INSTANCE.getVehicleAttachmentPoint(nmsPassenger, nmsVehicle));
            return passengerRidingPosition.subtract(vehicleAttachmentPoint);
        } else if (VersionHelper.isOrAbove1_20_2) {
            Vec3d passengerRidingPosition = LocationUtils.fromVec(EntityProxy.INSTANCE.getPassengerRidingPosition(nmsVehicle, nmsPassenger));
            return passengerRidingPosition.add(0, EntityProxy.INSTANCE.getMyRidingOffset(nmsVehicle, nmsPassenger), 0);
        } else {
            Vec3d pos = LocationUtils.fromVec(EntityProxy.INSTANCE.getPosition(nmsVehicle));
            return pos.add(0, EntityProxy.INSTANCE.getPassengersRidingOffset(nmsVehicle) + EntityProxy.INSTANCE.getMyRidingOffset(nmsPassenger), 0);
        }
    }

    public static BlockPos getOnPos(Player player) {
        Object serverPlayer = CraftEntityProxy.INSTANCE.getEntity(player);
        Object blockPos = EntityProxy.INSTANCE.getOnPos(serverPlayer);
        return LocationUtils.fromBlockPos(blockPos);
    }

    public static Entity spawnEntity(World world, Location loc, EntityType type, Consumer<Entity> function) {
        if (VersionHelper.isOrAbove1_20_2) {
            if (VersionHelper.hasPaperPatch) {
                return world.spawnEntity(loc, type, CreatureSpawnEvent.SpawnReason.CUSTOM, function);
            } else {
                return world.spawn(loc, type.getEntityClass(), (e) -> {
                    EntityProxy.INSTANCE.setRot(CraftEntityProxy.INSTANCE.getEntity(e), loc.getYaw(), loc.getPitch());
                    function.accept(e);
                });
            }
        } else {
            return LegacyEntityUtils.spawnEntity(world, loc, type, function);
        }
    }

    public static <T extends Entity> T spawnEntity(World world, Location loc, Class<T> type, Consumer<T> function) {
        if (VersionHelper.isOrAbove1_20_2) {
            if (VersionHelper.hasPaperPatch) {
                return world.spawn(loc, type, function);
            } else {
                return world.spawn(loc, type, (e) -> {
                    EntityProxy.INSTANCE.setRot(CraftEntityProxy.INSTANCE.getEntity(e), loc.getYaw(), loc.getPitch());
                    function.accept(e);
                });
            }
        } else {
            return LegacyEntityUtils.spawn(world, loc, type, function);
        }
    }

    public static Key getEntityType(Entity entity) {
        Object nmsEntity = CraftEntityProxy.INSTANCE.getEntity(entity);
        Object entityType = EntityProxy.INSTANCE.getType(nmsEntity);
        Object id = RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.ENTITY_TYPE, entityType);
        return KeyUtils.identifierToKey(id);
    }

    public static void safeDismount(Player player, Location location) {
        double boundBoxWidth = player.getBoundingBox().getWidthX();
        Location playerLocation = player.getLocation();
        Object serverLevel = BukkitAdaptor.adapt(player.getWorld()).minecraftWorld();
        Object serverPlayer = CraftEntityProxy.INSTANCE.getEntity(player);
        for (int i = 0; i < 8; i++) {
            Vec3d direction = getHorizontalDirection(i * 0.25, boundBoxWidth, playerLocation.getYaw());
            double x = location.getX() + direction.x;
            double y = location.getY();
            double z = location.getZ() + direction.z;
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
                Object newAABB = AABBProxy.INSTANCE.move$2(aabb, vec3);
                boolean canDismount = DismountHelperProxy.INSTANCE.canDismountTo(serverLevel, serverPlayer, newAABB);
                if (!canDismount) {
                    continue;
                }
                if (!CollisionUtils.test(serverLevel, List.of(newAABB), o -> true)) {
                    continue;
                }
                if (VersionHelper.hasFoliaPatch) {
                    player.teleportAsync(new Location(player.getWorld(), x, pos.y() + floorHeight, z, playerLocation.getYaw(), playerLocation.getPitch()));
                } else {
                    player.teleport(new Location(player.getWorld(), x, pos.y() + floorHeight, z, playerLocation.getYaw(), playerLocation.getPitch()));
                }

                if (pose == PoseProxy.STANDING) {
                    EntityProxy.INSTANCE.setPose(serverPlayer, PoseProxy.STANDING);
                } else if (pose == PoseProxy.CROUCHING) {
                    EntityProxy.INSTANCE.setPose(serverPlayer, PoseProxy.CROUCHING);
                } else if (pose == PoseProxy.SWIMMING) {
                    EntityProxy.INSTANCE.setPose(serverPlayer, PoseProxy.SWIMMING);
                }
                return;
            }
        }
        // 周围没有合适的落点时，如果玩家卡在方块内，向上至多 1 格寻找能容纳碰撞箱的位置，避免卡在地里
        dismountUpwards(player, serverLevel, serverPlayer, playerLocation);
    }

    private static void dismountUpwards(Player player, Object serverLevel, Object serverPlayer, Location playerLocation) {
        // 仅当玩家当前的碰撞箱与方块重叠时才尝试向上脱困
        Object currentAABB = EntityProxy.INSTANCE.getBoundingBox(serverPlayer);
        if (!CollisionGetterProxy.INSTANCE.getBlockCollisions(serverLevel, serverPlayer, currentAABB).iterator().hasNext()) {
            return;
        }
        double x = playerLocation.getX();
        double startY = playerLocation.getY();
        double z = playerLocation.getZ();
        Object aabb = LivingEntityProxy.INSTANCE.getLocalBoundsForPose(serverPlayer, PoseProxy.STANDING);
        for (int blockY = MiscUtils.floor(startY); blockY <= MiscUtils.floor(startY + 1); blockY++) {
            BlockPos pos = new BlockPos(MiscUtils.floor(x), blockY, MiscUtils.floor(z));
            double floorHeight = BlockGetterProxy.INSTANCE.getBlockFloorHeight(serverLevel, LocationUtils.toBlockPos(pos));
            double feetY = blockY + floorHeight;
            if (feetY <= startY || feetY - startY > 1 || Double.isInfinite(floorHeight)) {
                continue;
            }
            Object vec3 = Vec3Proxy.INSTANCE.newInstance(x, feetY, z);
            Object newAABB = AABBProxy.INSTANCE.move$2(aabb, vec3);
            if (!DismountHelperProxy.INSTANCE.canDismountTo(serverLevel, serverPlayer, newAABB)) {
                continue;
            }
            if (!CollisionUtils.test(serverLevel, List.of(newAABB), o -> false)) {
                continue;
            }
            if (VersionHelper.hasFoliaPatch) {
                player.teleportAsync(new Location(player.getWorld(), x, feetY, z, playerLocation.getYaw(), playerLocation.getPitch()));
            } else {
                player.teleport(new Location(player.getWorld(), x, feetY, z, playerLocation.getYaw(), playerLocation.getPitch()));
            }
            EntityProxy.INSTANCE.setPose(serverPlayer, PoseProxy.STANDING);
            return;
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

    public static <T> T getEntityDataValue(Object dataValue, EntityData<T> data) {
        try {
            return SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(dataValue);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Expected " + data + ", but got " + dataValue, e);
        }
    }

    public static Set<Player> getTrackedBy(Entity entity) {
        return getTrackedBySet(entity, p -> p);
    }

    public static <T> Set<T> getTrackedBySet(Entity entity, Function<Player, T> function) {
        ImmutableSet.Builder<T> players = ImmutableSet.builder();
        collectTrackedBy(entity, function, players::add);
        return players.build();
    }

    public static <T> List<T> getTrackedByList(Entity entity, Function<Player, T> function) {
        List<T> players = new ArrayList<>();
        collectTrackedBy(entity, function, players::add);
        return players;
    }

    @SuppressWarnings("deprecation")
    private static <T> void collectTrackedBy(Entity entity, Function<Player, T> function, Consumer<T> collector) {
        if (VersionHelper.hasPaperPatch) {
            Set<Player> trackedPlayers = entity.getTrackedPlayers();
            for (Player player : trackedPlayers) {
                T adapted = function.apply(player);
                if (adapted != null) {
                    collector.accept(adapted);
                }
            }
        } else {
            Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(entity.getWorld());
            Int2ObjectMap<Object> entityMap = ChunkMapProxy.INSTANCE.getEntityMap(ServerChunkCacheProxy.INSTANCE.getChunkMap(ServerLevelProxy.INSTANCE.getChunkSource(serverLevel)));
            Object tracker = entityMap.get(entity.getEntityId());
            if (tracker != null) {
                Set<Object> seenBy = ChunkMapProxy.TrackedEntityProxy.INSTANCE.getSeenBy(tracker);
                for (Object connection : seenBy) {
                    Object player = ServerPlayerConnectionProxy.INSTANCE.getPlayer(connection);
                    T adapted = function.apply((Player) PlayerProxy.INSTANCE.getBukkitEntity(player));
                    if (adapted != null) {
                        collector.accept(adapted);
                    }
                }
            }
        }
    }

    public static BukkitEntity adaptNMS(Object handle) {
        Class<?> clazz = handle.getClass();
        return (BukkitEntity) CACHED_ADAPTORS.computeIfAbsent(clazz, k -> {
            while (k != null) {
                Function<Object, net.momirealms.craftengine.core.entity.Entity> adaptor = ENTITY_ADAPTORS.get(k);
                if (adaptor != null) {
                    return adaptor;
                }
                k = k.getSuperclass();
            }
            throw new IllegalStateException("Could not find entity adaptor for " + clazz);
        }).apply(handle);
    }
}
