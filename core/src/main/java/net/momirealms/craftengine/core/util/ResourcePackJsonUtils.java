package net.momirealms.craftengine.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourcePackJsonUtils {

    private ResourcePackJsonUtils() {
    }

    public static JsonObject readJsonObjectOrWarn(PluginLogger logger, Path path) {
        try {
            JsonObject jsonObject = GsonHelper.readJsonObjectFromFile(path);
            if (jsonObject == null) {
                logger.warn(TranslationManager.instance().plainTranslation("resource_pack.not_a_json_object", path.toString()));
            }
            return jsonObject;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            logger.warn(TranslationManager.instance().plainTranslation("resource_pack.malformatted_json", path.toString()));
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static <T> T readJsonObjectOrWarn(PluginLogger logger, Path path, ObjectReader<T> objectReader) {
        boolean empty = true;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            // Match JsonParser: lenient document parsing, followed by a strict trailing-content check.
            reader.setLenient(true);
            JsonToken token = reader.peek();
            empty = false;
            if (token != JsonToken.BEGIN_OBJECT) {
                JsonElement element = JsonParser.parseReader(reader);
                reader.setLenient(false);
                if (!element.isJsonNull() && reader.peek() != JsonToken.END_DOCUMENT) {
                    throw new JsonParseException("Trailing JSON content");
                }
                logger.warn(TranslationManager.instance().plainTranslation("resource_pack.not_a_json_object", path.toString()));
                return null;
            }
            T result = objectReader.read(reader);
            reader.setLenient(false);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonParseException("Trailing JSON content");
            }
            return result;
        } catch (EOFException e) {
            logger.warn(TranslationManager.instance().plainTranslation(empty ? "resource_pack.not_a_json_object" : "resource_pack.malformatted_json", path.toString()));
        } catch (IOException | JsonParseException | IllegalStateException e) {
            logger.warn(TranslationManager.instance().plainTranslation("resource_pack.malformatted_json", path.toString()));
        }
        return null;
    }

    @FunctionalInterface
    public interface ObjectReader<T> {

        T read(JsonReader reader) throws IOException;
    }

    public static void writeJsonSafely(JsonElement json, Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            CraftEngine.instance().logger().error("Error creating " + path.toAbsolutePath(), e);
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            GsonHelper.get().toJson(json, writer);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to write json file " + json.toString() + " to " + path, e);
        }
    }
}
