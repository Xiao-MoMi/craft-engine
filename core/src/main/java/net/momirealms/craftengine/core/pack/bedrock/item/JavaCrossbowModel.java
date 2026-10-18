package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record JavaCrossbowModel(JavaItemModelLeaf idle, JavaItemModelLeaf pulling, List<JavaBowModel.Stage> stages,
                               float scale, JavaItemModelLeaf arrow, JavaItemModelLeaf rocket) {

    public static JavaCrossbowModel read(JsonObject json) throws IOException {
        if (JavaItemModelLeaf.type(json).equals("model")) {
            JavaItemModelLeaf leaf = JavaItemModelLeaf.read(json);
            return new JavaCrossbowModel(leaf, leaf, List.of(), 1, leaf, leaf);
        }
        if (!JavaItemModelLeaf.type(json).equals("select") || !JavaItemModelLeaf.name(json, "property").equals("charge_type")) {
            throw new IOException("Unsupported crossbow root: expected model or charge_type select");
        }
        JavaItemModelLeaf arrow = null;
        JavaItemModelLeaf rocket = null;
        for (JsonElement element : json.getAsJsonArray("cases")) {
            JsonObject entry = element.getAsJsonObject();
            JavaItemModelLeaf leaf = JavaItemModelLeaf.read(entry.getAsJsonObject("model"));
            for (String value : JavaDisplayContextModel.values(entry.get("when"))) {
                switch (value) {
                    case "arrow" -> arrow = leaf;
                    case "rocket" -> rocket = leaf;
                    default -> throw new IOException("Unsupported crossbow charge_type: " + value);
                }
            }
        }
        JsonObject fallback = json.getAsJsonObject("fallback");
        JavaItemModelLeaf idle;
        JavaItemModelLeaf pulling;
        List<JavaBowModel.Stage> stages = new ArrayList<>();
        float scale = 1;
        if (JavaItemModelLeaf.type(fallback).equals("model")) {
            idle = JavaItemModelLeaf.read(fallback);
            pulling = idle;
        } else {
            if (!JavaItemModelLeaf.type(fallback).equals("condition") || !JavaItemModelLeaf.name(fallback, "property").equals("using_item")) {
                throw new IOException("Unsupported crossbow fallback: expected model or using_item condition");
            }
            idle = JavaItemModelLeaf.read(fallback.getAsJsonObject("on_false"));
            JsonObject using = fallback.getAsJsonObject("on_true");
            if (JavaItemModelLeaf.type(using).equals("model")) {
                pulling = JavaItemModelLeaf.read(using);
            } else {
                if (!JavaItemModelLeaf.type(using).equals("range_dispatch") || !JavaItemModelLeaf.name(using, "property").equals("crossbow/pull")) {
                    throw new IOException("Unsupported crossbow pulling model: expected crossbow/pull range_dispatch");
                }
                pulling = JavaItemModelLeaf.read(using.getAsJsonObject("fallback"));
                scale = using.has("scale") ? using.get("scale").getAsFloat() : 1;
                if (!Float.isFinite(scale)) {
                    throw new IOException("Invalid crossbow pull scale");
                }
                for (JsonElement element : using.getAsJsonArray("entries")) {
                    JsonObject entry = element.getAsJsonObject();
                    float threshold = entry.get("threshold").getAsFloat();
                    if (!Float.isFinite(threshold)) {
                        throw new IOException("Invalid crossbow pull threshold");
                    }
                    stages.add(new JavaBowModel.Stage(threshold, JavaItemModelLeaf.read(entry.getAsJsonObject("model"))));
                }
                stages.sort(Comparator.comparingDouble(JavaBowModel.Stage::threshold));
            }
        }
        return new JavaCrossbowModel(idle, pulling, List.copyOf(stages), scale, arrow == null ? idle : arrow, rocket == null ? idle : rocket);
    }
}
