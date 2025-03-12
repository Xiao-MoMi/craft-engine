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

package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.StringReader;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockNbtParser {

    private BlockNbtParser() {}

    @Nullable
    public static CompoundTag deserialize(@NotNull CustomBlock block, @NotNull String data) {
        StringReader reader = new StringReader(data);
        CompoundTag properties = new CompoundTag();
        while (reader.canRead()) {
            String propertyName = reader.readUnquotedString();
            if (propertyName.isEmpty() || !reader.canRead() || reader.peek() != '=') {
                return null;
            }
            reader.skip();
            String propertyValue = reader.readUnquotedString();
            if (propertyValue.isEmpty()) {
                return null;
            }
            Property<?> property = block.getProperty(propertyName);
            if (property != null) {
                property.createOptionalTag(propertyValue).ifPresent(tag -> {
                    properties.put(propertyName, tag);
                });
            }
            if (reader.canRead() && reader.peek() == ',') {
                reader.skip();
            } else {
                break;
            }
        }
        return properties;
    }
}
