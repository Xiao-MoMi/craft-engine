package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

final class BedrockDefinitionIndex {
    final Map<Path, JsonObject> geometries = new HashMap<>();
    final Map<String, JsonObject> attachables = new LinkedHashMap<>();
    final Set<String> dirtyAttachables = new LinkedHashSet<>();
    final Set<String> indexedFiles = new HashSet<>();
    final Object2IntOpenHashMap<String> occurrences = new Object2IntOpenHashMap<>();

    void index(String path, JsonElement document) {
        this.indexedFiles.add(path);
        this.collect(document);
    }

    private void collect(JsonElement value) {
        if (value instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                this.collect(entry.getKey());
                this.collect(entry.getValue());
            }
        } else if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                this.collect(element);
            }
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            this.collect(value.getAsString());
        }
    }

    private void collect(String text) {
        if (text.contains("animation.ce.") || text.contains("controller.render.ce.")) {
            this.occurrences.addTo(text, 1);
        }
    }

    void flushAttachables(Path pack) throws IOException {
        for (String path : this.dirtyAttachables) {
            GsonHelper.writeJsonFile(this.attachables.get(path), pack.resolve(path));
        }
        this.dirtyAttachables.clear();
    }
}
