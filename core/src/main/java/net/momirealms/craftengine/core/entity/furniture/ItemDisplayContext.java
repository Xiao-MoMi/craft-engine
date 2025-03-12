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

public enum ItemDisplayContext {
    NONE(0),
    THIRD_PERSON_LEFT_HAND(1),
    THIRD_PERSON_RIGHT_HAND(2),
    FIRST_PERSON_LEFT_HAND(3),
    FIRST_PERSON_RIGHT_HAND(4),
    HEAD(5),
    GUI(6),
    GROUND(7),
    FIXED(8);

    private final byte id;

    ItemDisplayContext(int index) {
        this.id = (byte) index;
    }

    public byte id() {
        return id;
    }
}
