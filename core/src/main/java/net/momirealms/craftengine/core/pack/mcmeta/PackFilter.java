package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

public final class PackFilter {
    private final List<Block> block;

    public PackFilter(JsonObject filter) {
        JsonArray entries = filter.getAsJsonArray("block");
        List<Block> block = new ArrayList<>(entries.size());
        for (JsonElement entry : entries) {
            JsonObject pattern = entry.getAsJsonObject();
            block.add(new Block(
                    pattern.has("namespace") ? pattern.get("namespace").getAsString() : null,
                    pattern.has("path") ? pattern.get("path").getAsString() : null
            ));
        }
        this.block = List.copyOf(block);
    }

    public List<Block> block() {
        return this.block;
    }

    public BiPredicate<String, String> compile() {
        List<Pattern> namespaces = new ArrayList<>(this.block.size());
        List<Pattern> paths = new ArrayList<>(this.block.size());
        for (Block rule : this.block) {
            namespaces.add(Pattern.compile(rule.namespace() == null ? "" : rule.namespace()));
            paths.add(Pattern.compile(rule.path() == null ? "" : rule.path()));
        }
        // Java selects filtered namespaces first, then tests paths against the section's path patterns.
        return (namespace, path) -> matchesAny(namespaces, namespace) && matchesAny(paths, path);
    }

    private static boolean matchesAny(List<Pattern> patterns, String value) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    public record Block(@Nullable String namespace, @Nullable String path) {
    }
}
