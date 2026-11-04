package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Set;

record BedrockEquipmentPose(JsonArray scripts,
                            JsonArray bones,
                            JsonObject animations,
                            JsonArray animate,
                            Set<BedrockItemAttachables.View> views) {
    static final Set<BedrockItemAttachables.View> FIRST_PERSON_MAIN = Set.of(BedrockItemAttachables.View.FIRST_RIGHT);
    static final Set<BedrockItemAttachables.View> HANDS = Set.of(BedrockItemAttachables.View.FIRST_RIGHT, BedrockItemAttachables.View.FIRST_LEFT, BedrockItemAttachables.View.THIRD_RIGHT, BedrockItemAttachables.View.THIRD_LEFT);

    static BedrockEquipmentPose withoutBones(JsonArray scripts) {
        return new BedrockEquipmentPose(scripts, new JsonArray(), new JsonObject(), new JsonArray(), Set.of());
    }
}
