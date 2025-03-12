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

import java.util.function.Function;

public enum RotationRule {
    ANY(Function.identity()),
    FOUR(yaw -> (float) (Math.round(yaw / 90) * 90)),
    EIGHT(yaw -> (float) (Math.round(yaw / 45) * 45)),
    SIXTEEN(yaw -> (float) (Math.round(yaw / 22.5) * 22.5)),
    NORTH(__ -> 180f),
    EAST(__ -> -90f),
    WEST(__ -> 90f),
    SOUTH(__ -> 0f);

    private final Function<Float, Float> function;

    RotationRule(Function<Float, Float> function) {
        this.function = function;
    }

    public float apply(float yaw) {
        return function.apply(yaw);
    }
}
