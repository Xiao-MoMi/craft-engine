package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

final class VanillaItemModelDefaults {
    private static final String CUSTOM_MODEL_DATA = "custom_model_data";

    private VanillaItemModelDefaults() {
    }

    static @Nullable Selection selectRootFallback(JsonObject definition) {
        if (!(definition.get("model") instanceof JsonObject root) || !isCustomModelDataRange(root)) {
            return null;
        }

        JsonElement fallback = root.get("fallback");
        JsonElement transformation = root.get("transformation");
        JsonObject result = new JsonObject();
        definition.asMap().forEach(result::add);
        if (fallback != null && transformation == null) {
            result.add("model", fallback);
            return new Selection(result, false);
        }
        // 保留根节点的变换或缺失模型语义，编译时直接使用这一层的 fallback
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", "minecraft:range_dispatch");
        wrapper.addProperty("property", CUSTOM_MODEL_DATA);
        wrapper.add("entries", new JsonArray());
        if (fallback != null) {
            wrapper.add("fallback", fallback);
        }
        if (transformation != null) {
            wrapper.add("transformation", transformation);
        }
        result.add("model", wrapper);
        return new Selection(result, true);
    }

    private static boolean isCustomModelDataRange(JsonObject root) {
        return matches(root.get("type"), "range_dispatch", "minecraft:range_dispatch")
                && matches(root.get("property"), CUSTOM_MODEL_DATA, "minecraft:custom_model_data");
    }

    private static boolean matches(@Nullable JsonElement value, String name, String namespacedName) {
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isString()) {
            return false;
        }
        String key = primitive.getAsString();
        return name.equals(key) || namespacedName.equals(key);
    }

    record Selection(JsonObject definition, boolean useRootFallback) {
    }
}
