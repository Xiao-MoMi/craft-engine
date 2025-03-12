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

public class ResourceKey<T> {
    private final Key registry;
    private final Key location;

    public ResourceKey(Key registry, Key location) {
        this.registry = registry;
        this.location = location;
    }

    public static <T> ResourceKey<T> create(Key registry, Key location) {
        return new ResourceKey<>(registry, location);
    }

    public Key registry() {
        return registry;
    }

    public Key location() {
        return location;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + this.registry + " / " + this.location + "]";
    }
}
