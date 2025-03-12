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

package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.util.LightUtils;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.util.SectionPosUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.World;

public class BukkitCEWorld extends CEWorld {

    public BukkitCEWorld(World world) {
        super(world);
    }

    @Override
    public void tick() {
        if (ConfigManager.enableLightSystem()) {
            LightUtils.updateChunkLight((org.bukkit.World) world.getHandle(), SectionPosUtils.toMap(super.updatedSectionPositions, world.worldHeight().getMinSection() - 1, world.worldHeight().getMaxSection() + 1));
            super.updatedSectionPositions.clear();
        }
    }
}
