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
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FurnitureElement {
    private final Key item;
    private final Billboard billboard;
    private final ItemDisplayContext transform;
    private final Vector3f scale;
    private final Vector3f translation;
    private final Vector3f offset;
    private final Quaternionf rotation;

    public FurnitureElement(Key item, Billboard billboard, ItemDisplayContext transform, Vector3f scale, Vector3f translation, Vector3f offset, Quaternionf rotation) {
        this.billboard = billboard;
        this.transform = transform;
        this.scale = scale;
        this.translation = translation;
        this.item = item;
        this.rotation = rotation;
        this.offset = offset;
    }

    public Quaternionf rotation() {
        return rotation;
    }

    public Key item() {
        return item;
    }

    public Billboard billboard() {
        return billboard;
    }

    public ItemDisplayContext transform() {
        return transform;
    }

    public Vector3f scale() {
        return scale;
    }

    public Vector3f translation() {
        return translation;
    }

    public Vector3f offset() {
        return offset;
    }
}
