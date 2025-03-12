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
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.StringReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlockStateParser {

    @Nullable
    public static ImmutableBlockState deserialize(@NotNull String data) {
        StringReader reader = new StringReader(data);
        String blockIdString = reader.readUnquotedString();
        if (reader.canRead() && reader.peek() == ':') {
            reader.skip();
            blockIdString = blockIdString + ":" + reader.readUnquotedString();
        }
        Optional<Holder.Reference<CustomBlock>> optional = BuiltInRegistries.BLOCK.get(Key.from(blockIdString));
        if (optional.isEmpty()) {
            return null;
        }
        Holder<CustomBlock> holder = optional.get();
        ImmutableBlockState defaultState = holder.value().getDefaultState();
        if (reader.canRead() && reader.peek() == '[') {
            reader.skip();
            while (reader.canRead() && reader.peek() != ']') {
                String propertyName = reader.readUnquotedString();
                if (!reader.canRead() || reader.peek() != '=') {
                    return null;
                }
                reader.skip();
                String propertyValue = reader.readUnquotedString();
                Property<?> property = holder.value().getProperty(propertyName);
                if (property != null) {
                    Optional<?> optionalValue = property.optional(propertyValue);
                    if (optionalValue.isEmpty()) {
                        defaultState = ImmutableBlockState.with(defaultState, property, property.defaultValue());
                    } else {
                        defaultState = ImmutableBlockState.with(defaultState, property, optionalValue.get());
                    }
                }
                if (reader.canRead() && reader.peek() == ',') {
                    reader.skip();
                }
            }
            if (reader.canRead() && reader.peek() == ']') {
                reader.skip();
            } else {
                return null;
            }
        }
        return defaultState;
    }
}
