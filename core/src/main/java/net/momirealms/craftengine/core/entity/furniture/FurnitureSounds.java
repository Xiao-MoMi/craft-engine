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

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FurnitureSounds {
    public static final Key EMPTY_SOUND = Key.of("minecraft:intentionally_empty");
    public static final FurnitureSounds EMPTY = new FurnitureSounds(EMPTY_SOUND, EMPTY_SOUND, EMPTY_SOUND);

    private final Key breakSound;
    private final Key placeSound;
    private final Key rotateSound;

    public FurnitureSounds(Key breakSound, Key placeSound, Key rotateSound) {
        this.breakSound = breakSound;
        this.placeSound = placeSound;
        this.rotateSound = rotateSound;
    }

    public static FurnitureSounds fromMap(Map<String, Object> map) {
        if (map == null) return EMPTY;
        return new FurnitureSounds(
                Key.of(map.getOrDefault("break", "minecraft:intentionally_empty").toString()),
                Key.of(map.getOrDefault("place", "minecraft:intentionally_empty").toString()),
                Key.of(map.getOrDefault("rotate", "minecraft:intentionally_empty").toString())
        );
    }

    public Key breakSound() {
        return breakSound;
    }

    public Key placeSound() {
        return placeSound;
    }

    public Key rotateSound() {
        return rotateSound;
    }
}
