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

import net.momirealms.craftengine.core.entity.Entity;

public abstract class HitResult {
    protected final Vec3d location;

    protected HitResult(Vec3d pos) {
        this.location = pos;
    }

    public double distanceTo(Entity entity) {
        double d = this.location.x() - entity.x();
        double e = this.location.y() - entity.y();
        double f = this.location.z() - entity.z();
        return d * d + e * e + f * f;
    }

    public abstract HitResult.Type getType();

    public Vec3d getLocation() {
        return this.location;
    }

    public static enum Type {
        MISS,
        BLOCK,
        ENTITY;
    }
}
