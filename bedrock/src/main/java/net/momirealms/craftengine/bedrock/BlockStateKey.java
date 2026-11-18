package net.momirealms.craftengine.bedrock;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public record BlockStateKey(String identifier, Map<String, String> properties) {
    private static final Map<String, String> EMPTY_PROPERTIES = Collections.unmodifiableMap(new TreeMap<>());

    public BlockStateKey {
        properties = properties.isEmpty() ? EMPTY_PROPERTIES : Collections.unmodifiableMap(new TreeMap<>(properties));
    }

    public static BlockStateKey parse(String value) {
        String text = value.trim();
        int start = text.indexOf('[');
        String identifier = start < 0 ? text : text.substring(0, start).trim();
        int separator = identifier.indexOf(':');
        if (separator < 0) {
            identifier = "minecraft:" + identifier;
            separator = "minecraft".length();
        }
        if (!isIdentifier(identifier, separator)) {
            throw new IllegalArgumentException("Invalid block identifier: " + identifier);
        }
        if (start < 0) {
            return new BlockStateKey(identifier, EMPTY_PROPERTIES);
        }
        if (!text.endsWith("]")) {
            throw new IllegalArgumentException("Unclosed block state: " + value);
        }
        int bodyEnd = trimEnd(text, start + 1, text.length() - 1);
        int entryStart = trimStart(text, start + 1, bodyEnd);
        if (entryStart == bodyEnd) {
            return new BlockStateKey(identifier, EMPTY_PROPERTIES);
        }

        Map<String, String> properties = new TreeMap<>();
        while (true) {
            int comma = text.indexOf(',', entryStart, bodyEnd);
            int entryEnd = comma < 0 ? bodyEnd : comma;
            int equals = text.indexOf('=', entryStart, entryEnd);
            if (equals >= 0) {
                int nameStart = trimStart(text, entryStart, equals);
                int nameEnd = trimEnd(text, nameStart, equals);
                int valueStart = trimStart(text, equals + 1, entryEnd);
                int valueEnd = trimEnd(text, valueStart, entryEnd);
                if (isPropertyPart(text, nameStart, nameEnd, false) && isPropertyPart(text, valueStart, valueEnd, true)
                        && properties.putIfAbsent(text.substring(nameStart, nameEnd), text.substring(valueStart, valueEnd)) == null) {
                    if (comma < 0) {
                        return new BlockStateKey(identifier, properties);
                    }
                    entryStart = comma + 1;
                    continue;
                }
            }
            throw new IllegalArgumentException("Invalid block property: " + text.substring(entryStart, entryEnd));
        }
    }

    private static boolean isIdentifier(String text, int separator) {
        if (separator == 0 || separator == text.length() - 1) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (index != separator && !isNameCharacter(character) && character != '.' && character != '-' && !(index > separator && character == '/')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPropertyPart(String text, int start, int end, boolean allowHyphen) {
        for (int index = start; index < end; index++) {
            char character = text.charAt(index);
            if (!isNameCharacter(character) && !(allowHyphen && character == '-')) {
                return false;
            }
        }
        return start < end;
    }

    private static boolean isNameCharacter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= '0' && character <= '9') || character == '_';
    }

    private static int trimStart(String text, int start, int end) {
        while (start < end && text.charAt(start) <= ' ') {
            start++;
        }
        return start;
    }

    private static int trimEnd(String text, int start, int end) {
        while (end > start && text.charAt(end - 1) <= ' ') {
            end--;
        }
        return end;
    }

    public boolean matches(BlockStateKey state) {
        return this.identifier.equals(state.identifier) && state.properties.entrySet().containsAll(this.properties.entrySet());
    }

    @Override
    public String toString() {
        if (this.properties.isEmpty()) {
            return this.identifier;
        }
        StringJoiner joiner = new StringJoiner(",", this.identifier + "[", "]");
        this.properties.forEach((name, value) -> joiner.add(name + "=" + value));
        return joiner.toString();
    }
}
