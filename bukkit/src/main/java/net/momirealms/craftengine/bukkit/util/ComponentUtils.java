package net.momirealms.craftengine.bukkit.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.network.PacketConsumers;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.Map;

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

    public static void processComponent(Item<?> item, Key componentKey, boolean remap) {
        if (!item.hasComponent(componentKey)) return;
        String arrayField = switch (componentKey.toString()) {
            case "minecraft:can_break", "minecraft:can_place_on" -> "predicates";
            case "minecraft:tool" -> "rules";
            default -> "";
        };
        if (arrayField.isEmpty()) return;

        JsonObject component = (JsonObject) item.getJsonTypeComponent(componentKey);
        /*
        predicates:
          - blocks: craftengine:note_block_0
          - blocks: craftengine:note_block_1
          - blocks:
              - craftengine:note_block_2
              - craftengine:note_block_3
              - craftengine:note_block_4
        */
        if (component == null) return;

        JsonElement elements = component.get(arrayField);
        /*
        - blocks: craftengine:note_block_0
        - blocks: craftengine:note_block_1
        - blocks:
            - craftengine:note_block_2
            - craftengine:note_block_3
            - craftengine:note_block_4

        */
        if (!elements.isJsonArray()) return;
        if (elements.getAsJsonArray().isEmpty()) return;

        JsonArray newElements = processJsonArray(
                elements.getAsJsonArray(),
                remap,
                componentKey.toString(),
                item
        );

        component.add(arrayField, newElements);
        item.setComponent(componentKey, component);
    }

    private static JsonArray processJsonArray(JsonArray originalArray, boolean remap,
                                              String componentKey, Item<?> item) {
        JsonArray newArray = new JsonArray();
        /*
        - blocks: craftengine:note_block_0
        - blocks: craftengine:note_block_1
        - blocks:
            - craftengine:note_block_2
            - craftengine:note_block_3
            - craftengine:note_block_4

        */
        for (JsonElement element : originalArray) {
            // blocks: craftengine:note_block_0
            // 或者
            // blocks:
            //   - craftengine:note_block_2
            //   - craftengine:note_block_3
            //   - craftengine:note_block_4
            // components:
            //   ...
            // nbt: xxx
            // 或者
            // nbt:
            //   a: b
            // predicates:
            //   a: b
            // state:
            //   a: "b"
            //   b:
            //      min: "0"
            //      max: "6"
            if (!element.isJsonObject() || !element.getAsJsonObject().has("blocks")) {
                newArray.add(element);
                continue;
            }
            JsonObject processed = processBlock(
                    element.getAsJsonObject(),
                    remap,
                    componentKey,
                    item,
                    newArray
            );
            newArray.add(processed);
        }
        return newArray;
    }

    private static JsonObject processBlock(JsonObject blockObj, boolean remap,
                                           String componentKey, Item<?> item,
                                           JsonArray newArray) {
        JsonElement blocksElement = blockObj.get("blocks");
        // blocks: craftengine:note_block_0
        // 或者
        // blocks:
        //   - craftengine:note_block_2
        //   - craftengine:note_block_3
        //   - craftengine:note_block_4

        if (blocksElement.isJsonPrimitive()) {
            // craftengine:note_block_0
            processPrimitiveBlock(blockObj, blocksElement.getAsJsonPrimitive(),
                    remap, componentKey, item);
        } else if (blocksElement.isJsonArray()) {
            //  - craftengine:note_block_2
            //  - craftengine:note_block_3
            //  - craftengine:note_block_4
            processArrayBlock(blockObj, blocksElement.getAsJsonArray(),
                    remap, componentKey, item, newArray);
        }
        return blockObj;
    }

    private static void processPrimitiveBlock(JsonObject blockObj, JsonPrimitive primitive,
                                              boolean remap, String componentKey,
                                              Item<?> item) {
        if (!primitive.isString()) return;

        String value = primitive.getAsString();
        if (remap) {
            // craftengine:note_block_0
            JsonObject mapped = processRemap(value);
            if (mapped != null) {
                blockObj.remove("blocks");
                blockObj.add("blocks", mapped.get("blocks"));
                blockObj.add("state", mapped.get("state"));
            }
        } else {
            // minecraft:note_block
            processRestore(value, componentKey, item);
        }
    }

    private static void processArrayBlock(JsonObject blockObj, JsonArray blocksArray,
                                          boolean remap, String componentKey,
                                          Item<?> item, JsonArray newArray) {
        // if (!remap) blockObj.remove("blocks");
        JsonArray newBlocks = new JsonArray();
        //  - craftengine:note_block_2
        //  - craftengine:note_block_3
        //  - craftengine:note_block_4
        for (JsonElement block : blocksArray) {
            // craftengine:note_block_2
            if (block.isJsonPrimitive() && block.getAsJsonPrimitive().isString()) {
                String value = block.getAsString();
                if (remap) {
                    // craftengine:note_block_2
                    // minecraft:note_block
                    JsonObject mapped = processRemap(value);
                    if (mapped != null) {
                        JsonObject newBlockObj = new JsonObject();
                        newBlockObj.add("blocks", mapped.get("blocks"));
                        newBlockObj.add("state", mapped.get("state"));
                        newArray.add(newBlockObj);
                    } else {
                        newBlocks.add(block);
                    }
                } else {
                    // minecraft:note_block
                    processRestore(value, componentKey, item);
                }
            } else {
                newBlocks.add(block);
            }
        }
        blockObj.add("blocks", newBlocks);
    }

    private static JsonObject processRemap(String original) {
        // craftengine:note_block_0
        if (!original.startsWith("craftengine:")) return null;
        BlockData blockData = Bukkit.createBlockData(original);
        int id = BlockStateUtils.blockDataToId(blockData);
        int remapId = PacketConsumers.remap(id);
        Object remapBlockState = BlockStateUtils.idToBlockState(remapId);
        String remapBlockStateString = remapBlockState.toString();
        // Block{minecraft:note_block}[instrument=hat,note=1,powered=false]
        int blockFirst = remapBlockStateString.indexOf('{');
        int blockLast = remapBlockStateString.indexOf('}');
        if (!isValidIndices(blockFirst, blockLast)) return null;
        String blockName = remapBlockStateString.substring(blockFirst + 1, blockLast);
        int stateFirst = remapBlockStateString.indexOf('[');
        int stateLast = remapBlockStateString.lastIndexOf(']');
        if (!isValidIndices(stateFirst, stateLast)) return null;
        String state = remapBlockStateString.substring(stateFirst + 1, stateLast);
        JsonObject remapState = new JsonObject();
        int start = 0;
        int length = state.length();
        while (start < length) {
            int commaPos = state.indexOf(',', start);
            if (commaPos == -1) commaPos = length;
            int pairEnd = commaPos;
            String pair = state.substring(start, pairEnd);
            int equalPos = pair.indexOf('=');
            if (equalPos != -1) {
                String key = pair.substring(0, equalPos).trim();
                String value = pair.substring(equalPos + 1).trim();
                remapState.addProperty(key, value);
            }
            start = pairEnd + 1;
        }
        JsonObject remap = new JsonObject();
        remap.addProperty("blocks", blockName);
        remap.add("state", remapState);
        return remap;
    }

    private static void processRestore(String mapped, String componentKey, Item<?> item) {
        // minecraft:note_block
        if (!mapped.startsWith("minecraft:")) return;
        String arrayField = switch (componentKey) {
            case "minecraft:can_break", "minecraft:can_place_on" -> "predicates";
            case "minecraft:tool" -> "rules";
            default -> "";
        };
        if (arrayField.isEmpty()) return;
        JsonElement itemComponent = item.getJsonTypeComponent(Key.of(componentKey));
        if (itemComponent == null) return;
        if (!itemComponent.isJsonObject()) return;
        JsonObject itemComponentObj = itemComponent.getAsJsonObject();
        /*
        predicates:
          - blocks: minecraft:note_block
            state:
              instrument: "hat"
              note: "1“
              powered: "false"
          - blocks: minecraft:note_block
            state:
              instrument: "hat"
              note: "2“
              powered: "false"
        */
        JsonElement array = itemComponentObj.get(arrayField);
        /*
        - blocks: minecraft:note_block
          state:
            instrument: "hat"
            note: "1"
            powered: "false"
        - blocks: minecraft:note_block
          state:
            instrument: "hat"
            note: "2"
            powered: "false"
        */
        if (array == null || !array.isJsonArray()) return;
        for (JsonElement block : array.getAsJsonArray()) {
            if (!block.isJsonObject()) continue;
            JsonElement blocks = block.getAsJsonObject().get("blocks");
            if (blocks == null || !blocks.isJsonPrimitive() || !blocks.getAsJsonPrimitive().isString()) continue;
            if (!blocks.getAsString().equals(mapped)) continue;
            JsonElement state = block.getAsJsonObject().get("state");
            if (state == null || !state.isJsonObject()) continue;
            JsonObject stateObj = state.getAsJsonObject();
            StringBuilder stateString = new StringBuilder(blocks.getAsJsonPrimitive().getAsString());
            stateString.append("[");
            for (Map.Entry<String, JsonElement> entry : stateObj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
                String valueString = value.getAsString();
                stateString.append(key).append("=").append(valueString).append(",");
            }
            stateString.append("]");
            BlockData blockData = Bukkit.createBlockData(stateString.toString());
            int id = BlockStateUtils.blockDataToId(blockData);
            int remapId = PacketConsumers.reverseRemap(id);
            Object remapBlockState = BlockStateUtils.idToBlockState(remapId);
            Key blockOwnerId = BlockStateUtils.getBlockOwnerIdFromState(remapBlockState);
            if (blockOwnerId.namespace().equals("craftengine")) continue;
            block.getAsJsonObject().remove("blocks");
            block.getAsJsonObject().remove("state");
            block.getAsJsonObject().addProperty("blocks", blockOwnerId.toString());
        }
    }

    private static boolean isValidIndices(int colon, int underscore) {
        return colon != -1 && underscore != -1 && underscore > colon;
    }
}
