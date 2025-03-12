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

package net.momirealms.craftengine.core.util;

public enum Mirror {
    NONE,
    LEFT_RIGHT,
    FRONT_BACK;

    public int mirror(int rotation, int fullTurn) {
        int i = fullTurn / 2;
        int j = rotation > i ? rotation - fullTurn : rotation;
        return switch (this) {
            case LEFT_RIGHT -> (i - j + fullTurn) % fullTurn;
            case FRONT_BACK -> (fullTurn - j) % fullTurn;
            default -> rotation;
        };
    }

    public Rotation getRotation(Direction direction) {
        Direction.Axis axis = direction.axis();
        return (this != LEFT_RIGHT || axis != Direction.Axis.Z) && (this != FRONT_BACK || axis != Direction.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    public Direction mirror(Direction direction) {
        if (this == FRONT_BACK && direction.axis() == Direction.Axis.X) {
            return direction.opposite();
        } else {
            return this == LEFT_RIGHT && direction.axis() == Direction.Axis.Z ? direction.opposite() : direction;
        }
    }
}
