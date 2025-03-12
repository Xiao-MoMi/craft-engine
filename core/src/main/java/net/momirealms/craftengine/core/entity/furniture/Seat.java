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

package net.momirealms.craftengine.core.entity.furniture;

import org.joml.Vector3f;

import java.util.Objects;

public record Seat(Vector3f offset, float yaw, boolean limitPlayerRotation) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return Float.compare(yaw, seat.yaw) == 0 && Objects.equals(offset, seat.offset) && limitPlayerRotation == seat.limitPlayerRotation;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(offset);
        result = 31 * result + Float.hashCode(yaw);
        result = 31 * result + Boolean.hashCode(limitPlayerRotation);
        return result;
    }
}
