package net.momirealms.craftengine.core.util;

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

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

    public static JsonArray createArray(int first, int second) {
        JsonArray array = new JsonArray(2);
        array.add(first);
        array.add(second);
        return array;
    }

    public static JsonObject createObject(String key, JsonElement value) {
        JsonObject object = new JsonObject();
        object.add(key, value);
        return object;
    }

    public static JsonElement sortObjectKeys(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject result = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : new TreeMap<>(element.getAsJsonObject().asMap()).entrySet()) {
                result.add(entry.getKey(), sortObjectKeys(entry.getValue()));
            }
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement value : element.getAsJsonArray()) {
                result.add(sortObjectKeys(value));
            }
            return result;
        }
        return element.deepCopy();
    }

    public static boolean replaceStringValues(JsonElement value, Map<String, String> aliases) {
        return replaceStrings(value, aliases, Map.of());
    }

    public static boolean replaceStrings(JsonElement value, Map<String, String> values, Map<String, String> keys) {
        return replaceStrings(value, values::get, keys);
    }

    public static boolean replaceStrings(JsonElement value, UnaryOperator<String> values, Map<String, String> keys) {
        boolean changed = false;
        if (value instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String replacement = stringAlias(entry.getValue(), values);
                if (replacement != null) {
                    entry.setValue(new JsonPrimitive(replacement));
                    changed = true;
                } else {
                    changed |= replaceStrings(entry.getValue(), values, keys);
                }
            }
            changed |= replaceStringKeys(object, keys);
        } else if (value instanceof JsonArray array) {
            for (int i = 0; i < array.size(); i++) {
                JsonElement element = array.get(i);
                String replacement = stringAlias(element, values);
                if (replacement != null) {
                    array.set(i, new JsonPrimitive(replacement));
                    changed = true;
                } else {
                    changed |= replaceStrings(element, values, keys);
                }
            }
        }
        return changed;
    }

    public static boolean replaceStringKeys(JsonObject object, Map<String, String> aliases) {
        if (aliases.isEmpty() || object.keySet().stream().noneMatch(aliases::containsKey)) {
            return false;
        }
        Map<String, JsonElement> entries = LinkedHashMap.newLinkedHashMap(object.size());
        object.entrySet().forEach(entry -> entries.put(aliases.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue()));
        object.asMap().clear();
        object.asMap().putAll(entries);
        return true;
    }

    public static boolean replaceStringValues(JsonObject object, String key, Map<String, String> aliases) {
        JsonElement value = object.get(key);
        String replacement = stringAlias(value, aliases::get);
        if (replacement != null) {
            object.addProperty(key, replacement);
            return true;
        }
        return replaceStringValues(value, aliases);
    }

    private static String stringAlias(JsonElement value, UnaryOperator<String> aliases) {
        return value instanceof JsonPrimitive primitive && primitive.isString() ? aliases.apply(primitive.getAsString()) : null;
    }

    public static void writeJsonFile(JsonElement json, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writeJson(json, writer);
        }
    }

    public static void writeJsonFileWithNulls(JsonElement json, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            JsonTreeWriter jsonWriter = new JsonTreeWriter(writer);
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setStrictness(Strictness.LENIENT);
            GSON.getAdapter(JsonElement.class).write(jsonWriter, json);
        }
    }

    public static void writeJson(JsonElement json, Writer writer) {
        GSON.toJson(json, new JsonTreeWriter(writer));
    }

    public static JsonElement readJsonFromFile(Path path) throws IOException, JsonParseException {
        if (Files.size(path) <= 4096) {
            return JsonParser.parseString(Files.readString(path));
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8.newDecoder())) {
            return JsonParser.parseReader(reader);
        }
    }

    public static JsonElement readJsonFromResource(String path) {
        try (InputStream input = GsonHelper.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing bundled JSON resource " + path);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static JsonObject readJsonObjectFromFile(Path path) throws IOException, JsonParseException {
        JsonElement element = readJsonFromFile(path);
        return element instanceof JsonObject object ? object : null;
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

    private static final class JsonTreeWriter extends JsonWriter {
        private JsonTreeWriter(Writer writer) {
            super(writer);
        }

        @Override
        public JsonWriter value(Number value) throws IOException {
            if (value instanceof LazilyParsedNumber number) {
                // Preserve the original numeric text and avoid Gson allocating a matcher for each parsed number
                String text = number.toString();
                if (isJsonNumber(text)) {
                    return super.jsonValue(text);
                }
            }
            return super.value(value);
        }

        private static boolean isJsonNumber(String text) {
            int length = text.length();
            int index = text.startsWith("-") ? 1 : 0;
            if (index == length || !isDigit(text.charAt(index))) {
                return false;
            }
            index = text.charAt(index) == '0' ? index + 1 : skipDigits(text, index + 1);
            if (index < length && text.charAt(index) == '.') {
                int firstDigit = index + 1;
                index = skipDigits(text, firstDigit);
                if (index == firstDigit) {
                    return false;
                }
            }
            if (index < length && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                index++;
                if (index < length && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                    index++;
                }
                return index < length && skipDigits(text, index) == length;
            }
            return index == length;
        }

        private static int skipDigits(String text, int index) {
            while (index < text.length() && isDigit(text.charAt(index))) {
                index++;
            }
            return index;
        }

        private static boolean isDigit(char character) {
            return character >= '0' && character <= '9';
        }
    }
}
