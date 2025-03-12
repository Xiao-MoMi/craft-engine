/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class EntityUtils {

    private EntityUtils() {}

    public static BlockPos getOnPos(Player player) {
        try {
            Object serverPlayer = Reflections.method$CraftPlayer$getHandle.invoke(player);
            Object blockPos = Reflections.method$Entity$getOnPos.invoke(serverPlayer, 1.0E-5F);
            return new BlockPos(
                    (int) Reflections.field$Vec3i$x.get(blockPos),
                    (int) Reflections.field$Vec3i$y.get(blockPos),
                    (int) Reflections.field$Vec3i$z.get(blockPos)
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public static Entity spawnEntity(World world, Location loc, EntityType type, org.bukkit.util.Consumer<Entity> function) {
        try {
            return (Entity) Reflections.method$World$spawnEntity.invoke(world, loc, type, CreatureSpawnEvent.SpawnReason.CUSTOM, function);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to spawn entity", e);
        }
    }
}
