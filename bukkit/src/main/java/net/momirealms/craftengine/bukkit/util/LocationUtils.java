package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class LocationUtils {

    private LocationUtils() {}

    public static Location toLocation(WorldPosition position) {
        return new Location((World) position.world().platformWorld(), position.x(), position.y(), position.z(), position.yRot(), position.xRot());
    }

    public static WorldPosition toWorldPosition(Location location) {
        return new WorldPosition(BukkitAdaptors.adapt(location.getWorld()), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw());
    }

    public static Object toVec(Vec3d vec) {
        return Vec3Proxy.INSTANCE.newInstance(vec.x, vec.y, vec.z);
    }

    public static Vec3d toVec3d(Location loc) {
        return new Vec3d(loc.getX(), loc.getY(), loc.getZ());
    }

    public static Vec3d fromVec(Object vec) {
        return new Vec3d(
            FastNMS.INSTANCE.field$Vec3$x(vec),
            FastNMS.INSTANCE.field$Vec3$y(vec),
            FastNMS.INSTANCE.field$Vec3$z(vec)
        );
    }

    public static Object toBlockPos(BlockPos pos) {
        return toBlockPos(pos.x(), pos.y(), pos.z());
    }

    public static Object above(Object blockPos) {
        return toBlockPos(FastNMS.INSTANCE.field$Vec3i$x(blockPos), FastNMS.INSTANCE.field$Vec3i$y(blockPos) + 1, FastNMS.INSTANCE.field$Vec3i$z(blockPos));
    }

    public static Object above(Object blockPos, int y) {
        return toBlockPos(FastNMS.INSTANCE.field$Vec3i$x(blockPos), FastNMS.INSTANCE.field$Vec3i$y(blockPos) + y, FastNMS.INSTANCE.field$Vec3i$z(blockPos));
    }

    public static Object below(Object blockPos) {
        return toBlockPos(FastNMS.INSTANCE.field$Vec3i$x(blockPos), FastNMS.INSTANCE.field$Vec3i$y(blockPos) - 1, FastNMS.INSTANCE.field$Vec3i$z(blockPos));
    }

    public static Object below(Object blockPos, int y) {
        return toBlockPos(FastNMS.INSTANCE.field$Vec3i$x(blockPos), FastNMS.INSTANCE.field$Vec3i$y(blockPos) - y, FastNMS.INSTANCE.field$Vec3i$z(blockPos));
    }

    public static Object toBlockPos(int x, int y, int z) {
        return FastNMS.INSTANCE.constructor$BlockPos(x, y, z);
    }

    public static BlockPos toBlockPos(Location pos) {
        return new BlockPos(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
    }

    public static BlockPos fromBlockPos(Object pos) {
        return new BlockPos(
                FastNMS.INSTANCE.field$Vec3i$x(pos),
                FastNMS.INSTANCE.field$Vec3i$y(pos),
                FastNMS.INSTANCE.field$Vec3i$z(pos)
        );
    }

    public static Vec3d toVec3d(BlockPos pos) {
        return new Vec3d(pos.x(), pos.y(), pos.z());
    }

    public static double getDistance(Location location1, Location location2) {
        return Math.sqrt(Math.pow(location2.getX() - location1.getX(), 2) +
                Math.pow(location2.getY() - location1.getY(), 2) +
                Math.pow(location2.getZ() - location1.getZ(), 2)
        );
    }

    @NotNull
    public static Location toBlockLocation(Location location) {
        Location blockLoc = location.clone();
        blockLoc.setX(location.getBlockX());
        blockLoc.setY(location.getBlockY());
        blockLoc.setZ(location.getBlockZ());
        return blockLoc;
    }

    @NotNull
    public static Location toBlockCenterLocation(Location location) {
        Location centerLoc = location.clone();
        centerLoc.setX(location.getBlockX() + 0.5);
        centerLoc.setY(location.getBlockY() + 0.5);
        centerLoc.setZ(location.getBlockZ() + 0.5);
        return centerLoc;
    }

    @NotNull
    public static Location toSurfaceCenterLocation(Location location) {
        Location centerLoc = location.clone();
        centerLoc.setX(location.getBlockX() + 0.5);
        centerLoc.setZ(location.getBlockZ() + 0.5);
        centerLoc.setY(location.getBlockY());
        return centerLoc;
    }
}
