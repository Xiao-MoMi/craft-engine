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

import java.util.Random;

public enum Rotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    COUNTERCLOCKWISE_90;

    private static final Rotation[][] rotationMap = {
            {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90},
            {Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90, Rotation.NONE},
            {Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90, Rotation.NONE, Rotation.CLOCKWISE_90},
            {Rotation.COUNTERCLOCKWISE_90, Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180}
    };

    public Rotation getRotated(Rotation rotation) {
        int thisIndex = this.ordinal();
        int rotationIndex = rotation.ordinal();
        return rotationMap[thisIndex][rotationIndex];
    }

    public Direction rotate(Direction direction) {
        if (direction.axis() == Direction.Axis.Y) {
            return direction;
        } else {
            return switch (this) {
                case CLOCKWISE_90 -> direction.clockWise();
                case CLOCKWISE_180 -> direction.opposite();
                case COUNTERCLOCKWISE_90 -> direction.counterClockWise();
                default -> direction;
            };
        }
    }

    public int rotate(int rotation, int fullTurn) {
        return switch (this) {
            case CLOCKWISE_90 -> (rotation + fullTurn / 4) % fullTurn;
            case CLOCKWISE_180 -> (rotation + fullTurn / 2) % fullTurn;
            case COUNTERCLOCKWISE_90 -> (rotation + fullTurn * 3 / 4) % fullTurn;
            default -> rotation;
        };
    }

    public static Rotation random(Random random) {
        return values()[random.nextInt(values().length)];
    }
}
