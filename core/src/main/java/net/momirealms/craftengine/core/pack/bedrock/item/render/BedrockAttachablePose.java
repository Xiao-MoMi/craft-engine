package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Set;

record BedrockAttachablePose(JsonArray scripts,
                            JsonArray bones,
                            JsonObject animations,
                            JsonArray animate,
                            Set<BedrockItemAttachables.View> views) {
    static final Set<BedrockItemAttachables.View> FIRST_PERSON_MAIN = Set.of(BedrockItemAttachables.View.FIRST_RIGHT);
    static final Set<BedrockItemAttachables.View> HANDS = Set.of(BedrockItemAttachables.View.FIRST_RIGHT, BedrockItemAttachables.View.FIRST_LEFT, BedrockItemAttachables.View.THIRD_RIGHT, BedrockItemAttachables.View.THIRD_LEFT);

    static BedrockAttachablePose withoutBones(JsonArray scripts) {
        return new BedrockAttachablePose(scripts, new JsonArray(), new JsonObject(), new JsonArray(), Set.of());
    }
}
