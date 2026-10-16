package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record JavaItemModelLeaf(Key model, List<Integer> tints) {

    public static JavaItemModelLeaf read(JsonObject json) throws IOException {
        if (!type(json).equals("model")) {
            throw new IOException("Unsupported item definition type: " + type(json));
        }
        JsonElement reference = json.get("model");
        if (reference == null || !reference.isJsonPrimitive()) {
            throw new IOException("Missing static model reference");
        }
        List<Integer> tints = List.of();
        if (json.has("tints") && !json.getAsJsonArray("tints").isEmpty()) {
            tints = new ArrayList<>(json.getAsJsonArray("tints").size());
            for (JsonElement entry : json.getAsJsonArray("tints")) {
                JsonObject tint = entry.getAsJsonObject();
                if (!type(tint).equals("constant")) {
                    throw new IOException("Unsupported dynamic item tint: " + type(tint));
                }
                tints.add(tint.get("value").getAsInt() & 0xFFFFFF);
            }
            tints = List.copyOf(tints);
        }
        return new JavaItemModelLeaf(Key.of(reference.getAsString()), tints);
    }

    static String type(JsonObject json) {
        return name(json, "type");
    }

    static String name(JsonObject json, String field) {
        String value = json.has(field) ? json.get(field).getAsString() : "";
        return value.startsWith("minecraft:") ? value.substring(10) : value;
    }
}
