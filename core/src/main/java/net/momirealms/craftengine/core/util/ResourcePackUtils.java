package net.momirealms.craftengine.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourcePackUtils {
    private ResourcePackUtils() {
    }

    public static String blockStateSelector(String state) {
        int start = state.indexOf('[');
        return start < 0 ? "" : state.substring(start + 1, state.length() - 1);
    }

    public static void writeJson(JsonElement json, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(json, path);
    }

    public static void writeBedrockJson(String version, String key, JsonElement value, Path path) throws IOException {
        JsonObject file = new JsonObject();
        file.addProperty(BedrockPackLayout.FORMAT_VERSION, version);
        file.add(key, value);
        writeJson(file, path);
    }

    public static String selectBedrockResource(JsonObject controller, String type, String name, JsonArray entries, String index) {
        if (!entries.isEmpty()) {
            JsonElement first = entries.get(0);
            int i = 1;
            while (i < entries.size() && first.equals(entries.get(i))) {
                i++;
            }
            if (i == entries.size()) {
                return first.getAsString();
            }
        }
        JsonObject arrays = controller.asMap().computeIfAbsent("arrays", key -> new JsonObject()).getAsJsonObject();
        JsonObject group = arrays.asMap().computeIfAbsent(type, key -> new JsonObject()).getAsJsonObject();
        group.add(name, entries);
        return name + "[" + index + "]";
    }

    public static String vanillaTexturePath(String texture) {
        return "assets/minecraft/textures/" + texture;
    }

    public static String texturePath(Key texture) {
        return "assets/" + texture.namespace() + "/textures/" + texture.value() + ".png";
    }

    public static String itemDefinitionPath(Key key) {
        return "assets/" + key.namespace() + "/items/" + key.value() + ".json";
    }

    public static String modelPath(Key key) {
        return "assets/" + key.namespace() + "/models/" + key.value() + ".json";
    }

    public static String particleDefinitionPath(Key key) {
        return "assets/" + key.namespace() + "/particles/" + key.value() + ".json";
    }

    public static String bedrockResourceIdentifier(Key key) {
        String source = key.asString();
        StringBuilder identifier = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            switch (character) {
                case ':', '/' -> identifier.append('.');
                default -> identifier.append(character);
            }
        }
        return identifier.toString();
    }
}
