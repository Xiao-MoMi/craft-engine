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

package net.momirealms.craftengine.core.pack.obfuscation;

public enum ResourceType {
    SOUND("sounds", ".ogg"),
    BLOCKSTATE("blockstates", ".json"),
    FONT("font", ".json"),
    ITEM("items", ".json"),
    MODEL("models", ".json"),
    TEXTURE("textures", ".png");

    private final String typeName;
    private final String suffix;

    ResourceType(String typeName, String suffix) {
        this.typeName = typeName;
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public String typeName() {
        return typeName;
    }

    public static ResourceType of(String typeName) {
        for (ResourceType type : values()) {
            if (type.typeName.equals(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + typeName);
    }
}
