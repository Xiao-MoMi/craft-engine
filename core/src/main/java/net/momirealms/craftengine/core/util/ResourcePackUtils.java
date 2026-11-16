package net.momirealms.craftengine.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourcePackUtils {

    private ResourcePackUtils() {
    }

    public static void writeJson(JsonElement json, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(json, path);
    }

    public static void writeBedrockJson(String version, String key, JsonElement value, Path path) throws IOException {
        JsonObject file = new JsonObject();
        file.addProperty("format_version", version);
        file.add(key, value);
        writeJson(file, path);
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
