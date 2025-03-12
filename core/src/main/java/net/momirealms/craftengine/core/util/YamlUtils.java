package net.momirealms.craftengine.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlUtils {

    private YamlUtils() {}

    public static Map<String, Object> sectionToMap(final Section section) {
        Map<String, Object> map = new LinkedHashMap<>();
        sectionToMap(section, map);
        return map;
    }

    private static void sectionToMap(final Section section, final Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Section inner) {
                HashMap<String, Object> newMap = new LinkedHashMap<>();
                map.put(entry.getKey(), newMap);
                sectionToMap(inner, newMap);
            } else {
                if (value instanceof String str && !str.isEmpty()) {
                    char first = str.charAt(0);
                    if (first == '(') {
                        int second = str.indexOf(')', 1);
                        if (second != -1 && second + 1 + 1 < str.length()) {
                            char space = str.charAt(second + 1);
                            if (space == ' ') {
                                String castType = str.substring(1, second);
                                String castValue = str.substring(second + 2);
                                map.put(entry.getKey(), TypeUtils.castBasicTypes(castValue, castType));
                                continue;
                            }
                        }
                    }
                }
                map.put(entry.getKey(), value);
            }
        }
    }

    public static JsonObject sectionToJson(final Section section) {
        JsonObject json = new JsonObject();
        sectionToJson(section, json);
        return json;
    }

    private static void sectionToJson(final Section section, final JsonObject json) {
        for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Section inner) {
                JsonObject innerObject = new JsonObject();
                json.add(key, innerObject);
                sectionToJson(inner, innerObject);
            } else if (value instanceof List<?> list) {
                JsonArray array = new JsonArray();
                json.add(key, array);
                for (Object o : list) {
                    setJsonArray(array, o);
                }
            } else {
                setJsonProperty(json, key, value);
            }
        }
    }

    public static JsonObject mapToJson(final Map<String, Object> map) {
        return GsonHelper.get().toJsonTree(map).getAsJsonObject();
    }

    private static void setJsonArray(final JsonArray array, final Object value) {
        if (value instanceof Map<?,?> map) {
            JsonObject jsonObject = GsonHelper.get().toJsonTree(map).getAsJsonObject();
            array.add(jsonObject);
        } else if (value instanceof Number number) {
            array.add(number);
        } else if (value instanceof Boolean bool) {
            array.add(bool);
        } else if (value instanceof Character character) {
            array.add(character);
        } else {
            array.add(value.toString());
        }
    }

    private static void setJsonProperty(final JsonObject json, final String key, final Object value) {
        if (value instanceof Number number) {
            json.addProperty(key, number);
        } else if (value instanceof Boolean bool) {
            json.addProperty(key, bool);
        } else if (value instanceof Character character) {
            json.addProperty(key, character);
        } else {
            json.addProperty(key, value.toString());
        }
    }
}
