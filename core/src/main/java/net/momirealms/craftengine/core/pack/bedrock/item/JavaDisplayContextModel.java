package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record JavaDisplayContextModel(JavaItemModelLeaf fallback, Map<DisplayPosition, JavaItemModelLeaf> contexts) {

    public JavaDisplayContextModel {
        contexts = Map.copyOf(contexts);
    }

    public JavaItemModelLeaf model(DisplayPosition position) {
        return this.contexts.getOrDefault(position, this.fallback);
    }

    public static JavaDisplayContextModel read(JsonObject json) throws IOException {
        if (JavaItemModelLeaf.type(json).equals("model")) {
            return new JavaDisplayContextModel(JavaItemModelLeaf.read(json), Map.of());
        }
        if (!JavaItemModelLeaf.type(json).equals("select") || !JavaItemModelLeaf.name(json, "property").equals("display_context")) {
            throw new IOException("Unsupported spear root: expected model or display_context select");
        }
        Map<DisplayPosition, JavaItemModelLeaf> contexts = new EnumMap<>(DisplayPosition.class);
        for (JsonElement element : json.getAsJsonArray("cases")) {
            JsonObject entry = element.getAsJsonObject();
            JavaItemModelLeaf leaf = JavaItemModelLeaf.read(entry.getAsJsonObject("model"));
            for (String value : values(entry.get("when"))) {
                if (value.equals("on_shelf") || value.equals("none")) {
                    continue;
                }
                DisplayPosition position = DisplayPosition.valueOf(value.toUpperCase(java.util.Locale.ROOT));
                contexts.putIfAbsent(position, leaf);
            }
        }
        return new JavaDisplayContextModel(JavaItemModelLeaf.read(json.getAsJsonObject("fallback")), contexts);
    }

    static List<String> values(JsonElement json) {
        if (!json.isJsonArray()) {
            return List.of(json.getAsString());
        }
        List<String> result = new ArrayList<>(json.getAsJsonArray().size());
        for (JsonElement element : json.getAsJsonArray()) {
            result.add(element.getAsString());
        }
        return result;
    }
}
