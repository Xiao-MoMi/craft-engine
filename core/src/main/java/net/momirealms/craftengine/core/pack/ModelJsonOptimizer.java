package net.momirealms.craftengine.core.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

final class ModelJsonOptimizer {
    private static final String[] REDUNDANT_MODEL_TAGS = {"credit", "groups", "texture_size", "format_version", "__comment", "__comment__", "_comment"};
    private static final JsonPrimitive JSON_TRUE = new JsonPrimitive(true);
    private static final JsonPrimitive JSON_FALSE = new JsonPrimitive(false);
    private static final JsonPrimitive JSON_ZERO = new JsonPrimitive(0);
    private static final JsonPrimitive JSON_NEGATIVE_ONE = new JsonPrimitive(-1);
    private static final JsonPrimitive JSON_EMPTY_STRING = new JsonPrimitive("");

    private ModelJsonOptimizer() {
    }

    static void optimize(JsonObject object) {
        for (String key : REDUNDANT_MODEL_TAGS) {
            object.remove(key);
        }
        JsonElement elements = object.get("elements");
        if (elements != null && elements.isJsonArray()) {
            JsonArray elementArray = elements.getAsJsonArray();
            for (int index = 0; index < elementArray.size(); index++) {
                JsonElement element = elementArray.get(index);
                if (element.isJsonObject()) {
                    optimizeModelElement(element.getAsJsonObject());
                }
            }
        }
    }

    private static void optimizeModelElement(JsonObject element) {
        element.remove("name");
        element.remove("color");
        element.remove("rotated");
        if (JSON_TRUE.equals(element.get("shade"))) {
            element.remove("shade");
        }
        if (JSON_ZERO.equals(element.get("light_emission"))) {
            element.remove("light_emission");
        }
        JsonElement rotation = element.get("rotation");
        if (rotation != null && rotation.isJsonObject()) {
            JsonObject rotationObject = rotation.getAsJsonObject();
            if (JSON_FALSE.equals(rotationObject.get("rescale"))) {
                rotationObject.remove("rescale");
            }
        }
        JsonElement faces = element.get("faces");
        if (faces != null && faces.isJsonObject()) {
            for (JsonElement face : faces.getAsJsonObject().asMap().values()) {
                if (face.isJsonObject()) {
                    JsonObject faceObject = face.getAsJsonObject();
                    if (JSON_ZERO.equals(faceObject.get("rotation"))) {
                        faceObject.remove("rotation");
                    }
                    if (JSON_NEGATIVE_ONE.equals(faceObject.get("tintindex"))) {
                        faceObject.remove("tintindex");
                    }
                    if (JSON_EMPTY_STRING.equals(faceObject.get("cullface"))) {
                        faceObject.remove("cullface");
                    }
                }
            }
        }
    }
}
