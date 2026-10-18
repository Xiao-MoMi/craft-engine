package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;

import java.io.IOException;

public record JavaFishingRodModel(JavaItemModelLeaf idle, JavaItemModelLeaf cast) {

    public static JavaFishingRodModel read(JsonObject json) throws IOException {
        if (JavaItemModelLeaf.type(json).equals("model")) {
            JavaItemModelLeaf model = JavaItemModelLeaf.read(json);
            return new JavaFishingRodModel(model, model);
        }
        if (!JavaItemModelLeaf.type(json).equals("condition") || !JavaItemModelLeaf.name(json, "property").equals("fishing_rod/cast")) {
            throw new IOException("Unsupported fishing rod root: expected model or fishing_rod/cast condition");
        }
        return new JavaFishingRodModel(JavaItemModelLeaf.read(json.getAsJsonObject("on_false")),
                JavaItemModelLeaf.read(json.getAsJsonObject("on_true")));
    }
}
