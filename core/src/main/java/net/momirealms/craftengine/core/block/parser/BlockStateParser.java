package net.momirealms.craftengine.core.block.parser;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.StringReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class BlockStateParser {
    private static final char START = '[';
    private static final char EQUAL = '=';
    private static final char SEPARATOR = ',';
    private static final char END = ']';

    private final StringReader reader;
    private final int cursor;
    private final Set<String> suggestions = new HashSet<>();
    private final Set<String> used = new HashSet<>();

    private String input;
    private int replaceCursor;
    private Holder<CustomBlock> block;
    private Collection<Property<?>> properties;
    private Property<?> property;

    public BlockStateParser(String data, int cursor) {
        this.reader = StringReader.simple(data.toLowerCase());
        this.reader.setCursor(cursor);
        this.cursor = cursor;
        this.replaceCursor = cursor;
    }

    public static Set<String> fillSuggestions(@NotNull String data) {
        return fillSuggestions(data, 0);
    }

    public static Set<String> fillSuggestions(@NotNull String data, int cursor) {
        BlockStateParser parser = new BlockStateParser(data, cursor);
        parser.parse();
        return parser.suggestions;
    }

    private void parse() {
        readBlock();
        if (block == null) {
            suggestBlock();
            return;
        }

        readProperties();
        if (properties.isEmpty()) return;

        if (!reader.canRead())
            suggestStart();
        else if (reader.peek() == START) {
            reader.skip();
            suggestProperties();
        }
    }

    private void readBlock() {
        this.replaceCursor = reader.getCursor();
        this.input = reader.readUnquotedString();
        if (reader.canRead() && reader.peek() == ':') {
            reader.skip();
            input = input + ":" + reader.readUnquotedString();
        }
        BuiltInRegistries.BLOCK.get(Key.from(input)).ifPresent(block -> this.block = block);
    }

    private void suggestBlock() {
        String front = readPrefix();
        for (Key key : BuiltInRegistries.BLOCK.keySet()) {
            String id = key.toString();
            if (id.contains(input)) {
                this.suggestions.add(front + id);
            }
        }
        this.suggestions.remove(front + "craftengine:empty");
    }

    private void readProperties() {
        this.properties = this.block.value().properties();
    }

    private void suggestStart() {
        this.replaceCursor = reader.getCursor();
        this.suggestions.add(readPrefix() + START);
    }

    private void suggestProperties() {
        this.reader.skipWhitespace();
        this.replaceCursor = reader.getCursor();
        suggestPropertyNameAndEnd();

        while (reader.canRead()) {
            if (used.isEmpty() && reader.peek() == SEPARATOR) return;
            if (reader.peek() == SEPARATOR) reader.skip();
            reader.skipWhitespace();
            if (reader.canRead() && reader.peek() == END) return;

            replaceCursor = reader.getCursor();
            input = reader.readString();

            property = block.value().getProperty(input);
            if (property == null) {
                suggestPropertyName();
                return;
            }
            if (used.contains(property.name().toLowerCase())) return;
            used.add(input);

            reader.skipWhitespace();
            replaceCursor = reader.getCursor();
            suggestEqual();

            if (!reader.canRead() || reader.peek() != EQUAL) return;

            reader.skip();
            reader.skipWhitespace();
            replaceCursor = reader.getCursor();
            input = reader.readString();
            if (property.possibleValues().stream().noneMatch
                    (value -> value.toString().equalsIgnoreCase(input))
            ){
                suggestValue();
                return;
            }

            reader.skipWhitespace();
            replaceCursor = reader.getCursor();
            if (reader.canRead()) {
                if (used.size() == properties.size()) return;
                if (reader.peek() != SEPARATOR) return;
            } else if (used.size() < properties.size()) {
                suggestSeparator();
            }
        }
        suggestEnd();
    }

    private void suggestPropertyNameAndEnd() {
        if (!reader.getRemaining().isEmpty()) return;
        this.input = "";
        suggestEnd();
        suggestPropertyName();

    }
    private void suggestPropertyName() {
        if (!reader.getRemaining().isEmpty()) return;
        String front = readPrefix();
        for (Property<?> p : properties) {
            if (!used.contains(p.name().toLowerCase()) && p.name().toLowerCase().startsWith(input)) {
                this.suggestions.add(front + p.name() + EQUAL);
            }
        }
    }

    private void suggestEqual() {
        if (!reader.getRemaining().isEmpty()) return;
        this.suggestions.add(readPrefix() + EQUAL);
    }

    private void suggestValue() {
        for (Object val : property.possibleValues()) {
            this.suggestions.add(readPrefix() + val.toString().toLowerCase());
        }
    }

    private void suggestSeparator() {
        this.suggestions.add(readPrefix() + SEPARATOR);
    }

    private void suggestEnd() {
        this.suggestions.add(readPrefix() + END);
    }

    private String readPrefix() {
        return reader.getString().substring(cursor, replaceCursor);
    }

    @Nullable
    public static ImmutableBlockState deserialize(@NotNull String data) {
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
        ImmutableBlockState defaultState = holder.value().defaultState();
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
                Property<?> property = holder.value().getProperty(propertyName);
                if (property != null) {
                    Optional<?> optionalValue = property.optional(propertyValue);
                    if (optionalValue.isEmpty()) {
                        //defaultState = ImmutableBlockState.with(defaultState, property, property.defaultValue());
                        return null;
                    } else {
                        defaultState = ImmutableBlockState.with(defaultState, property, optionalValue.get());
                    }
                } else {
                    return null;
                }
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
        return defaultState;
    }
}
