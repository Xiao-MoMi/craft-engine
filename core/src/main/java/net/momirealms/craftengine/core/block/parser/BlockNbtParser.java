package net.momirealms.craftengine.core.block.parser;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.StringReader;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockNbtParser {
    private BlockNbtParser() {}

    @Nullable
    public static CompoundTag deserialize(@NotNull CustomBlock block, @NotNull String data) {
        StringReader reader = StringReader.simple(data);
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
