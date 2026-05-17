package net.momirealms.craftengine.core.block.parser;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateVariantProvider;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.StringReader;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class BlockNbtParser {
    private BlockNbtParser() {}

    @Nullable
    public static CompoundTag deserialize(@NotNull Function<String, Property<?>> propertyProvider, @NotNull String data) {
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
            Property<?> property = propertyProvider.apply(propertyName);
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

    @Nullable
    public static CompoundTag deserialize(@NotNull BlockDefinition block, @NotNull String data) {
        return deserialize(block::getProperty, data);
    }

    @Nullable
    public static CompoundTag deserialize(@NotNull BlockStateVariantProvider variantProvider, @NotNull String data) {
        return deserialize(variantProvider::getProperty, data);
    }
}
