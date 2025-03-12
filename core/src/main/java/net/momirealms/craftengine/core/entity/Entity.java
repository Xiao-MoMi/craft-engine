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

package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.World;

public abstract class Entity {

    public abstract double x();

    public abstract double y();

    public abstract double z();

    public abstract void tick();

    public abstract int entityID();

    public abstract float getXRot();

    public abstract float getYRot();

    public abstract World level();

    public abstract Direction getDirection();
}
