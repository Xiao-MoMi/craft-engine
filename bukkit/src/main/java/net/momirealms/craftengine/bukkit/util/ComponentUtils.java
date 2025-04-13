package net.momirealms.craftengine.bukkit.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

public class ComponentUtils {

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

    public static void processComponent(Item<?> item, Key componentKey, String arrayField) {
        if (!item.hasComponent(componentKey)) return;
        JsonObject component = (JsonObject) item.getJsonTypeComponent(componentKey);
        if (component == null) return;
        JsonElement elements = component.get(arrayField);
        if (!elements.isJsonArray()) return;
        JsonArray newElements = processJsonArray(elements.getAsJsonArray());
        component.add(arrayField, newElements);
        item.setComponent(componentKey, component);
    }

    private static JsonArray processJsonArray(JsonArray originalArray) {
        JsonArray newArray = new JsonArray();
        for (JsonElement element : originalArray) {
            if (!element.isJsonObject()) {
                newArray.add(element);
                continue;
            }
            newArray.add(processBlock(element.getAsJsonObject()));
        }
        return newArray;
    }

    private static JsonObject processBlock(JsonObject blockObj) {
        JsonElement blocksElement = blockObj.get("blocks");
        if (blocksElement.isJsonPrimitive()) {
            processBlockPrimitive(blockObj, blocksElement.getAsJsonPrimitive());
        } else if (blocksElement.isJsonArray()) {
            processBlockArray(blockObj, blocksElement.getAsJsonArray());
        }
        return blockObj;
    }

    private static void processBlockPrimitive(JsonObject blockObj, JsonPrimitive primitive) {
        if (!primitive.isString()) return;
        String processed = processBlockName(primitive.getAsString());
        if (processed != null) {
            blockObj.add("blocks", new JsonPrimitive(processed));
        }
    }

    private static void processBlockArray(JsonObject blockObj, JsonArray blocksArray) {
        JsonArray newBlocks = new JsonArray();
        for (JsonElement block : blocksArray) {
            if (block.isJsonPrimitive() && block.getAsJsonPrimitive().isString()) {
                String processed = processBlockName(block.getAsString());
                if (processed != null) {
                    newBlocks.add(processed);
                } else {
                    newBlocks.add(block);
                }
            } else {
                newBlocks.add(block);
            }
        }
        blockObj.add("blocks", newBlocks);
    }

    private static String processBlockName(String originalName) {
        if (!originalName.startsWith("craftengine:")) return null;
        int colonIndex = originalName.indexOf(':');
        int underscoreIndex = originalName.lastIndexOf('_');
        if (isValidIndices(colonIndex, underscoreIndex)) {
            return "minecraft:" + originalName.substring(colonIndex + 1, underscoreIndex);
        }
        return null;
    }

    private static boolean isValidIndices(int colonIndex, int underscoreIndex) {
        return colonIndex != -1 && underscoreIndex != -1 && underscoreIndex > colonIndex;
    }
}
