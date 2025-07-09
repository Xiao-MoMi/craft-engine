package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.StringReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class UnsafeBlockStateMatcher {
    private final List<Pair<String, String>> matchers;
    private final Key id;

    public UnsafeBlockStateMatcher(Key id, List<Pair<String, String>> matchers) {
        this.id = id;
        this.matchers = matchers;
    }

    public boolean matches(ImmutableBlockState state) {
        if (!state.owner().value().id().equals(this.id)) {
            return false;
        }
        CustomBlock customBlock = state.owner().value();
        for (Pair<String, String> matcher : matchers) {
            Property<?> property = customBlock.getProperty(matcher.left());
            if (property == null) {
                return false;
            }
            Comparable<?> value = state.get(property);
            String valueStr = Property.formatValue(property, value);
            if (!matcher.right().equals(valueStr)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (matchers.isEmpty()) {
            return id.toString();
        }
        return id + "[" + matchers.stream()
                .map(entry -> entry.left() + "=" + entry.right())
                .collect(Collectors.joining(",")) + "]";
    }

    @SuppressWarnings("DuplicatedCode")
    @Nullable
    public static UnsafeBlockStateMatcher deserialize(@NotNull String data) {
        StringReader reader = StringReader.simple(data);
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
        List<Pair<String, String>> properties = new ArrayList<>();
        if (reader.canRead() && reader.peek() == '[') {
            reader.skip();
            while (reader.canRead()) {
                reader.skipWhitespace();
                if (reader.peek() == ']') break;
                String propertyName = reader.readUnquotedString();
                reader.skipWhitespace();
                if (!reader.canRead() || reader.peek() != '=') {
                    return null;
                }
                reader.skip();
                reader.skipWhitespace();
                String propertyValue = reader.readUnquotedString();
                properties.add(new Pair<>(propertyName, propertyValue));
                reader.skipWhitespace();
                if (reader.canRead() && reader.peek() == ',') {
                    reader.skip();
                }
            }
            reader.skipWhitespace();
            if (reader.canRead() && reader.peek() == ']') {
                reader.skip();
            } else {
                return null;
            }
        }
        return new UnsafeBlockStateMatcher(holder.value().id(), properties);
    }
}
