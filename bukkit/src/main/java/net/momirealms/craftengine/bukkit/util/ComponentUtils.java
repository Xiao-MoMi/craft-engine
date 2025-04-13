package net.momirealms.craftengine.bukkit.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentUtils {
    private static final String BLOCK_MAPPINGS_KEY = "craftengine:block_mappings";
    private static final String CRAFTENGINE_PREFIX = "craftengine:";
    private static final String MINECRAFT_PREFIX = "minecraft:";

    private ComponentUtils() {}

    public static Object adventureToMinecraft(Component component) {
        return jsonElementToMinecraft(AdventureHelper.componentToJsonElement(component));
    }

    public static Object jsonElementToMinecraft(JsonElement json) {
        return FastNMS.INSTANCE.method$Component$Serializer$fromJson(json);
    }

    public static Object jsonToMinecraft(String json) {
        return FastNMS.INSTANCE.method$Component$Serializer$fromJson(json);
    }

    public static String minecraftToJson(Object component) {
        return FastNMS.INSTANCE.method$Component$Serializer$toJson(component);
    }

    public static String paperAdventureToJson(Object component) {
        return Reflections.instance$GsonComponentSerializer$Gson.toJson(component);
    }

    public static Object jsonToPaperAdventure(String json) {
        return Reflections.instance$GsonComponentSerializer$Gson.fromJson(json, Reflections.clazz$AdventureComponent);
    }

    public static JsonElement paperAdventureToJsonElement(Object component) {
        return Reflections.instance$GsonComponentSerializer$Gson.toJsonTree(component);
    }

    public static Object jsonElementToPaperAdventure(JsonElement json) {
        return Reflections.instance$GsonComponentSerializer$Gson.fromJson(json, Reflections.clazz$AdventureComponent);
    }

    public static void processComponent(Item<?> item, Key componentKey, String arrayField, boolean remap) {
        if (!item.hasComponent(componentKey)) return;

        JsonObject component = (JsonObject) item.getJsonTypeComponent(componentKey);
        if (component == null) return;

        JsonElement elements = component.get(arrayField);
        if (!elements.isJsonArray()) return;

        Map<String, List<String>> mappings = new HashMap<>();

        JsonArray newElements = processJsonArray(
                elements.getAsJsonArray(),
                remap,
                componentKey.toString(),
                item,
                mappings
        );

        component.add(arrayField, newElements);
        item.setComponent(componentKey, component);

        if (remap) {
            if (!mappings.isEmpty()) {
                System.out.println("Remapped " + componentKey + ": " + mappings);
                updateBlockMappings(item, componentKey.toString(), mappings);
            }
        } else {
            cleanComponentMappings(item, componentKey.toString());
        }
    }

    private static void cleanComponentMappings(Item<?> item, String componentKey) {
        JsonObject customData = (JsonObject) item.getJsonTypeComponent(ComponentKeys.CUSTOM_DATA);
        if (customData == null) return;
        JsonObject allMappings = customData.getAsJsonObject(BLOCK_MAPPINGS_KEY);
        if (allMappings == null) return;
        allMappings.remove(componentKey);
        if (allMappings.isEmpty()) {
            customData.remove(BLOCK_MAPPINGS_KEY);
        }
        if (customData.isEmpty()) {
            item.removeComponent(ComponentKeys.CUSTOM_DATA);
        } else {
            item.setComponent(ComponentKeys.CUSTOM_DATA, customData);
        }
    }

    private static JsonArray processJsonArray(JsonArray originalArray, boolean remap,
                                              String componentKey, Item<?> item,
                                              Map<String, List<String>> mappings) {
        JsonArray newArray = new JsonArray();
        for (JsonElement element : originalArray) {
            if (!element.isJsonObject()) {
                newArray.add(element);
                continue;
            }
            JsonObject processed = processBlock(
                    element.getAsJsonObject(),
                    remap,
                    componentKey,
                    item,
                    mappings
            );
            newArray.add(processed);
        }
        return newArray;
    }

    private static JsonObject processBlock(JsonObject blockObj, boolean remap,
                                           String componentKey, Item<?> item,
                                           Map<String, List<String>> mappings) {
        JsonElement blocksElement = blockObj.get("blocks");

        if (blocksElement.isJsonPrimitive()) {
            processPrimitiveBlock(blockObj, blocksElement.getAsJsonPrimitive(),
                    remap, componentKey, item, mappings);
        } else if (blocksElement.isJsonArray()) {
            processArrayBlock(blockObj, blocksElement.getAsJsonArray(),
                    remap, componentKey, item, mappings);
        }
        return blockObj;
    }

    private static void processPrimitiveBlock(JsonObject blockObj, JsonPrimitive primitive,
                                              boolean remap, String componentKey,
                                              Item<?> item, Map<String, List<String>> mappings) {
        if (!primitive.isString()) return;

        String value = primitive.getAsString();
        if (remap) {
            String mapped = processRemap(value, mappings);
            if (mapped != null) blockObj.add("blocks", new JsonPrimitive(mapped));
        } else {
            JsonArray restored = processRestore(value, componentKey, item);
            if (restored != null) blockObj.addProperty("blocks", restored.get(0).getAsString());
        }
    }

    private static void processArrayBlock(JsonObject blockObj, JsonArray blocksArray,
                                          boolean remap, String componentKey,
                                          Item<?> item, Map<String, List<String>> mappings) {
        if (!remap) blockObj.remove("blocks");
        JsonArray newBlocks = new JsonArray();
        for (JsonElement block : blocksArray) {
            if (block.isJsonPrimitive() && block.getAsJsonPrimitive().isString()) {
                String value = block.getAsString();
                if (remap) {
                    String mapped = processRemap(value, mappings);
                    newBlocks.add(mapped != null ? mapped : value);
                } else {
                    JsonArray restored = processRestore(value, componentKey, item);
                    if (restored != null) restored.forEach(newBlocks::add);
                    else newBlocks.add(value);
                }
            } else {
                newBlocks.add(block);
            }
        }
        blockObj.add("blocks", newBlocks);
    }

    private static String processRemap(String original,
                                       Map<String, List<String>> mappings) {
        if (!original.startsWith(CRAFTENGINE_PREFIX)) return null;

        int colonIndex = original.indexOf(':');
        int underscoreIndex = original.lastIndexOf('_');

        if (!isValidIndices(colonIndex, underscoreIndex)) return null;

        String mapped = MINECRAFT_PREFIX + original.substring(colonIndex+1, underscoreIndex);

        mappings.computeIfAbsent(mapped, k -> new ArrayList<>())
                .add(original);

        return mapped;
    }

    private static JsonArray processRestore(String mapped, String componentKey, Item<?> item) {
        if (!mapped.startsWith(MINECRAFT_PREFIX)) return null;

        JsonObject mappings = getBlockMappings(item, componentKey);
        if (mappings == null) return null;

        JsonArray origins = mappings.getAsJsonArray(mapped);
        if (origins == null || origins.isEmpty()) return null;

        JsonArray result = new JsonArray();
        origins.forEach(result::add);
        return result;
    }

    private static void updateBlockMappings(Item<?> item, String componentKey,
                                            Map<String, List<String>> newMappings) {
        JsonObject customData = getOrCreateCustomData(item);
        JsonObject allMappings = getOrCreateMappings(customData);

        JsonObject componentMappings = allMappings.has(componentKey)
                ? allMappings.getAsJsonObject(componentKey)
                : new JsonObject();

        newMappings.forEach((mapped, origins) -> {
            JsonArray existing = componentMappings.has(mapped)
                    ? componentMappings.getAsJsonArray(mapped)
                    : new JsonArray();

            origins.stream()
                    .filter(origin -> !contains(existing, origin))
                    .forEach(existing::add);

            componentMappings.add(mapped, existing);
        });

        allMappings.add(componentKey, componentMappings);
        customData.add(BLOCK_MAPPINGS_KEY, allMappings);
        item.setComponent(ComponentKeys.CUSTOM_DATA, customData);
    }

    private static JsonObject getOrCreateCustomData(Item<?> item) {
        return item.hasComponent(ComponentKeys.CUSTOM_DATA)
                ? (JsonObject) item.getJsonTypeComponent(ComponentKeys.CUSTOM_DATA)
                : new JsonObject();
    }

    private static JsonObject getOrCreateMappings(JsonObject customData) {
        return customData.has(BLOCK_MAPPINGS_KEY)
                ? customData.getAsJsonObject(BLOCK_MAPPINGS_KEY)
                : new JsonObject();
    }

    private static JsonObject getBlockMappings(Item<?> item, String componentKey) {
        JsonObject customData = (JsonObject) item.getJsonTypeComponent(ComponentKeys.CUSTOM_DATA);
        if (customData == null) return null;

        JsonObject allMappings = customData.getAsJsonObject(BLOCK_MAPPINGS_KEY);
        if (allMappings == null) return null;

        return allMappings.getAsJsonObject(componentKey);
    }

    private static boolean contains(JsonArray array, String value) {
        return array.asList().stream()
                .anyMatch(e -> e.isJsonPrimitive() && e.getAsString().equals(value));
    }

    private static boolean isValidIndices(int colon, int underscore) {
        return colon != -1 && underscore != -1 && underscore > colon;
    }
}
