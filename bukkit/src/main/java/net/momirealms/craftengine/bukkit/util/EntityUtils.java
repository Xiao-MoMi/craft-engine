package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.function.Consumer;

public final class EntityUtils {

    private EntityUtils() {
    }

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

    public static Key getEntityType(Entity entity) {
        Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(entity);
        Object entityType = FastNMS.INSTANCE.method$Entity$getType(nmsEntity);
        Object id = FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ENTITY_TYPE, entityType);
        return KeyUtils.resourceLocationToKey(id);
    }
}