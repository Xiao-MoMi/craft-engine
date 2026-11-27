package net.momirealms.craftengine.core.pack.bedrock.item.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

final class BedrockPlayerAnimationState {
    private final JsonObject state;
    private final JsonArray animations;

    BedrockPlayerAnimationState(JsonObject state) {
        this.state = state;
        this.animations = state.getAsJsonArray("animations");
    }

    void updateCondition(String name, UnaryOperator<String> change) {
        this.updateConditions(name, null, false, change);
    }

    void updateFirstCondition(String name, UnaryOperator<String> change) {
        this.updateConditions(name, null, true, change);
    }

    void updateConditionUnlessPresent(String name, String preferred, UnaryOperator<String> change) {
        this.updateConditions(name, preferred, false, change);
    }

    private void updateConditions(String name, @Nullable String preferred, boolean firstOnly, UnaryOperator<String> change) {
        for (JsonElement animation : this.animations) {
            if (animation instanceof JsonObject entry && entry.has(name) && (preferred == null || !entry.has(preferred))) {
                entry.addProperty(name, change.apply(entry.get(name).getAsString()));
                if (firstOnly) {
                    return;
                }
            }
        }
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
