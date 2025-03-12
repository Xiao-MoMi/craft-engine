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

package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.UUID;

public interface World {

    Object getHandle();

    WorldHeight worldHeight();

    WorldBlock getBlockAt(int x, int y, int z);

    default WorldBlock getBlockAt(final BlockPos pos) {
        return getBlockAt(pos.x(), pos.y(), pos.z());
    }

    String name();

    Path directory();

    UUID uuid();

    void dropItemNaturally(Vec3d location, Item<?> item);

    void dropExp(Vec3d location, int amount);

    void playBlockSound(Vec3d location, Key sound, float volume, float pitch);
}
