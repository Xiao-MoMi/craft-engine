package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record JavaBowModel(JavaItemModelLeaf idle, JavaItemModelLeaf pulling, List<Stage> stages, float scale, boolean remaining) {

    public static JavaBowModel read(JsonObject json) throws IOException {
        if (JavaItemModelLeaf.type(json).equals("model")) {
            JavaItemModelLeaf model = JavaItemModelLeaf.read(json);
            return new JavaBowModel(model, model, List.of(), 1, false);
        }
        if (!JavaItemModelLeaf.type(json).equals("condition") || !JavaItemModelLeaf.name(json, "property").equals("using_item")) {
            throw new IOException("Unsupported bow root: expected model or using_item condition");
        }
        JavaItemModelLeaf idle = JavaItemModelLeaf.read(json.getAsJsonObject("on_false"));
        JsonObject using = json.getAsJsonObject("on_true");
        if (JavaItemModelLeaf.type(using).equals("model")) {
            return new JavaBowModel(idle, JavaItemModelLeaf.read(using), List.of(), 1, false);
        }
        if (!JavaItemModelLeaf.type(using).equals("range_dispatch") || !JavaItemModelLeaf.name(using, "property").equals("use_duration")) {
            throw new IOException("Unsupported pulling model: expected model or use_duration range_dispatch");
        }
        if (!using.has("fallback")) {
            throw new IOException("Bow use_duration requires a fallback model");
        }
        float scale = using.has("scale") ? using.get("scale").getAsFloat() : 1;
        if (!Float.isFinite(scale)) {
            throw new IOException("Invalid bow use_duration scale");
        }
        boolean remaining = using.has("remaining") && using.get("remaining").getAsBoolean();
        List<Stage> stages = new ArrayList<>();
        for (JsonElement element : using.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            float threshold = entry.get("threshold").getAsFloat();
            if (!Float.isFinite(threshold)) {
                throw new IOException("Invalid bow use_duration threshold");
            }
            stages.add(new Stage(threshold, JavaItemModelLeaf.read(entry.getAsJsonObject("model"))));
        }
        stages.sort(Comparator.comparingDouble(Stage::threshold));
        return new JavaBowModel(idle, JavaItemModelLeaf.read(using.getAsJsonObject("fallback")), List.copyOf(stages), scale, remaining);
    }

    public record Stage(float threshold, JavaItemModelLeaf model) {
    }
}
