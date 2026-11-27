package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class BedrockDisplaySharing {
    private BedrockDisplaySharing() {
    }

    static boolean share(Path root, JsonObject entity) throws IOException {
        JsonObject description = entity.getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        Path file = root.resolve("render_controllers/item_display.json");
        if (!description.has("identifier") || !(BedrockPackLayout.ID_PREFIX + "item_display").equals(description.get("identifier").getAsString()) || !Files.isRegularFile(file)) {
            return false;
        }
        JsonObject document = GsonHelper.readJsonObjectFromFile(file);
        JsonObject controllers = document.getAsJsonObject("render_controllers");
        // 这些短名只在 CE 自有展示实体的控制器中使用。其他实体和外部控制器保留其自己的名称域。
        for (JsonElement value : description.getAsJsonArray("render_controllers")) {
            if (!value.isJsonObject()) {
                return false;
            }
            for (String id : value.getAsJsonObject().keySet()) {
                if (!id.startsWith("controller.render.item_display.") || !controllers.has(id)) {
                    return false;
                }
            }
        }
        Map<String, String> aliases = new HashMap<>();
        collectAliases(description.getAsJsonObject("geometry"), "Geometry.", "empty", aliases);
        collectAliases(description.getAsJsonObject("textures"), "Texture.", "default", aliases);
        if (aliases.isEmpty()) {
            return false;
        }
        GsonHelper.replaceStringValues(controllers, aliases);
        for (JsonElement value : controllers.asMap().values()) {
            JsonObject controller = value.getAsJsonObject();
            JsonObject arrays = controller.getAsJsonObject("arrays");
            if (arrays != null) {
                collapse(controller, arrays, "geometries", "geometry", "array.meshes");
                collapse(controller, arrays, "textures", "textures", "array.frames");
                if (arrays.isEmpty()) {
                    controller.remove("arrays");
                }
            }
        }
        GsonHelper.writeJsonFile(document, file);
        return true;
    }

    private static void collectAliases(JsonObject names, String prefix, String reserved, Map<String, String> aliases) {
        Map<String, String> targets = new HashMap<>();
        targets.put(names.get(reserved).getAsString(), reserved);
        Iterator<Map.Entry<String, JsonElement>> iterator = names.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = iterator.next();
            String name = entry.getKey();
            String existing = targets.putIfAbsent(entry.getValue().getAsString(), name);
            if (existing != null && !name.equals(existing)) {
                aliases.put(prefix + name, prefix + existing);
                iterator.remove();
            }
        }
    }

    private static void collapse(JsonObject controller, JsonObject arrays, String group, String property, String name) {
        JsonObject definitions = arrays.getAsJsonObject(group);
        if (definitions == null || !definitions.has(name)) {
            return;
        }
        JsonArray values = definitions.getAsJsonArray(name);
        JsonElement first = values.get(0);
        for (JsonElement value : values) {
            if (!first.equals(value)) {
                return;
            }
        }
        JsonElement reference = controller.get(property);
        JsonElement expression = reference.isJsonArray() ? reference.getAsJsonArray().get(0) : reference;
        if (!expression.getAsString().startsWith(name + "[")) {
            return;
        }
        if (reference.isJsonArray()) {
            reference.getAsJsonArray().set(0, first);
        } else {
            controller.add(property, first);
        }
        definitions.remove(name);
        if (definitions.isEmpty()) {
            arrays.remove(group);
        }
    }
}
