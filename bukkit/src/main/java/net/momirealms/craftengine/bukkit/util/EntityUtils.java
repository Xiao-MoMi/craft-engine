package net.momirealms.craftengine.bukkit.util;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EntityUtils {

    private EntityUtils() {}

    public static BlockPos getOnPos(Player player) {
        try {
            Object serverPlayer = FastNMS.INSTANCE.method$CraftPlayer$getHandle(player);
            Object blockPos = CoreReflections.method$Entity$getOnPos.invoke(serverPlayer, 1.0E-5F);
            return LocationUtils.fromBlockPos(blockPos);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Entity spawnEntity(World world, Location loc, EntityType type, Consumer<Entity> function) {
        if (VersionHelper.isOrAbove1_20_2()) {
            return world.spawnEntity(loc, type, CreatureSpawnEvent.SpawnReason.CUSTOM, function);
        } else {
            return LegacyEntityUtils.spawnEntity(world, loc, type, function);
        }
    }

    @Nullable
    public static Object buildTeleportPacket(int entityId, boolean onGround, double x, double y, double z, float yRot, float xRot) {
        try {
            if (VersionHelper.isOrAbove1_21_2()) {
                Object position = CoreReflections.constructor$Vec3.newInstance(x, y, z);
                Object positionMoveRotation = FastNMS.INSTANCE.constructor$PositionMoveRotation(position, CoreReflections.instance$Vec3$Zero, yRot, xRot);
                return NetworkReflections.constructor$ClientboundEntityPositionSyncPacket.newInstance(entityId, positionMoveRotation, onGround);
            }
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeVarInt(entityId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeByte(MCUtils.packDegrees(yRot));
            buf.writeByte(MCUtils.packDegrees(xRot));
            buf.writeBoolean(onGround);
            return NetworkReflections.constructor$ClientboundTeleportEntityPacket.newInstance(FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf));
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to build teleport packet", e);
            return null;
        }
    }
}
