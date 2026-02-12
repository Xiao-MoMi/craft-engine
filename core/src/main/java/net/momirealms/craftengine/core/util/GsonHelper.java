package net.momirealms.craftengine.core.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class GsonHelper {
    private static final Gson GSON;

    private GsonHelper() {}

    static {
        GSON = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
    }

    public static Gson get() {
        return GSON;
    }

    public static void writeJsonFile(JsonElement json, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            get().toJson(json, writer);
        }
    }

    public static JsonElement readJsonFile(Path path) throws IOException, JsonParseException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }

    public static JsonElement parseJson(String json) {
        return GSON.fromJson(json, JsonElement.class);
    }

    public static String toString(JsonElement json) {
        return GSON.toJson(json);
    }

    public static boolean isCompactJson(String content) {
        String trimmed = content.trim();
        return !trimmed.contains("\n") && !trimmed.contains("\r") &&
                content.length() == trimmed.length();
    }

    public static JsonObject shallowMerge(JsonObject obj1, JsonObject obj2) {
        JsonObject merged = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : obj1.entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, JsonElement> entry : obj2.entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    public static JsonObject deepMerge(JsonObject source, JsonObject target) {
        JsonObject merged = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, JsonElement> entry : target.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (merged.has(key)) {
                JsonElement existingValue = merged.get(key);
                if (existingValue.isJsonObject() && value.isJsonObject()) {
                    JsonObject mergedChild = deepMerge(
                            existingValue.getAsJsonObject(),
                            value.getAsJsonObject()
                    );
                    merged.add(key, mergedChild);
                } else {
                    merged.add(key, value);
                }
            } else {
                merged.add(key, value);
            }
        }
        return merged;
    }

    public static JsonObject parseJsonToJsonObject(String json) {
        try {
            return get().fromJson(
                    json,
                    JsonObject.class
            );
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Invalid JSON response: " + json, e);
        }
    }

    public static Map<String, Object> parseJsonToMap(String json) {
        try {
            return GsonHelper.get().fromJson(
                    json,
                    new TypeToken<Map<String, Object>>() {}.getType()
            );
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Invalid JSON response: " + json, e);
        }
    }

    public static Map<String, Object> parseJsonToMap(Reader json) {
        return GsonHelper.get().fromJson(
                json,
                new TypeToken<Map<String, Object>>() {}.getType()
        );
    }

    public static JsonElement combine(List<? extends JsonElement> jo) {
        if (jo.size() == 1) {
            return jo.getFirst();
        } else {
            JsonArray ja = new JsonArray();
            for (JsonElement je : jo) {
                ja.add(je);
            }
            return ja;
        }
    }

    public static float getAsFloat(JsonElement json, float defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsFloat();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getAsDouble(JsonElement json, double defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getAsInt(JsonElement json, int defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getAsLong(JsonElement json, long defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsLong();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static short getAsShort(JsonElement json, short defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsShort();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static byte getAsByte(JsonElement json, byte defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsByte();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getAsBoolean(JsonElement json, boolean defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getAsString(JsonElement json, String defaultValue) {
        if (json == null || json.isJsonNull()) return defaultValue;
        try {
            return json.getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
