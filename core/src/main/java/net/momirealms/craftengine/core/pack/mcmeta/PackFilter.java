package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    public record Block(@Nullable String namespace, @Nullable String path) {
    }
}
