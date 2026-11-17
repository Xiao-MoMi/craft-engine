package net.momirealms.craftengine.bedrock;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public record BlockStateKey(String identifier, Map<String, String> properties) {
    public BlockStateKey {
        properties = Collections.unmodifiableMap(new TreeMap<>(properties));
    }

    public static BlockStateKey parse(String value) {
        String text = value.trim();
        int start = text.indexOf('[');
        String identifier = start < 0 ? text : text.substring(0, start).trim();
        if (!identifier.contains(":")) {
            identifier = "minecraft:" + identifier;
        }
        if (!identifier.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid block identifier: " + identifier);
        }
        Map<String, String> properties = new TreeMap<>();
        if (start >= 0) {
            if (!text.endsWith("]")) {
                throw new IllegalArgumentException("Unclosed block state: " + value);
            }
            String body = text.substring(start + 1, text.length() - 1).trim();
            if (!body.isEmpty()) {
                for (String entry : body.split(",", -1)) {
                    String[] pair = entry.split("=", -1);
                    if (pair.length != 2 || !pair[0].trim().matches("[a-z0-9_]+") || !pair[1].trim().matches("[a-z0-9_-]+")
                            || properties.putIfAbsent(pair[0].trim(), pair[1].trim()) != null) {
                        throw new IllegalArgumentException("Invalid block property: " + entry);
                    }
                }
            }
        }
        return new BlockStateKey(identifier, properties);
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
