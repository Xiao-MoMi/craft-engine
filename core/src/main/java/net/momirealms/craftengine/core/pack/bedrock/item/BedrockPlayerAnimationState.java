package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

final class BedrockPlayerAnimationState {
    private final JsonObject state;
    private final JsonArray animations;

    BedrockPlayerAnimationState(JsonObject state) {
        this.state = state;
        this.animations = state.getAsJsonArray("animations");
    }

    private Stream<JsonObject> entriesWith(String name) {
        return this.animations.asList().stream().filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject).filter(entry -> entry.has(name));
    }

    void updateCondition(String name, UnaryOperator<String> change) {
        this.entriesWith(name).forEach(entry -> replaceCondition(entry, name, change));
    }

    void updateFirstCondition(String name, UnaryOperator<String> change) {
        this.entriesWith(name).findFirst().ifPresent(entry -> replaceCondition(entry, name, change));
    }

    void updateConditionUnlessPresent(String name, String preferred, UnaryOperator<String> change) {
        this.entriesWith(name).filter(entry -> !entry.has(preferred)).forEach(entry -> replaceCondition(entry, name, change));
    }

    private static void replaceCondition(JsonObject entry, String name, UnaryOperator<String> change) {
        entry.addProperty(name, change.apply(entry.get(name).getAsString()));
    }

    void appendAnimation(String name, String condition) {
        JsonObject entry = new JsonObject();
        entry.addProperty(name, condition);
        this.animations.add(entry);
    }

    void setBooleanVariable(String name, String condition) {
        JsonObject curve = new JsonObject();
        curve.addProperty("0.0", 0);
        curve.addProperty("1.0", 1);
        JsonObject variable = new JsonObject();
        variable.addProperty("input", condition);
        variable.add("remap_curve", curve);
        JsonObject variables = this.state.asMap().computeIfAbsent("variables", ignored -> new JsonObject()).getAsJsonObject();
        variables.add(name, variable);
    }
}
