package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.pack.obfuscation.ResourceKey;
import net.momirealms.craftengine.core.pack.obfuscation.ResourceType;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class JsonUtils {
    private static final Gson gson = new Gson();

    public static boolean isJsonFile(Path filePath, boolean strict) {
        if (!Files.isRegularFile(filePath)) return false;
        if (!strict) {
            String fileName = filePath.getFileName().toString();
            return fileName.endsWith(".json") || fileName.endsWith(".mcmeta");
        } else {
            try {
                gson.fromJson(Files.readString(filePath), Object.class);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static String jsonUnicode(JsonElement jsonData) {
        JsonElement processedData = processData(jsonData);
        String json = gson.toJson(processedData);
        return json.replace("\\\\u", "\\u");
    }

    private static String escapeToUnicode(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            result.append(String.format("\\u%04x", (int) c));
        }
        return result.toString();
    }

    private static JsonElement processData(JsonElement input) {
        if (input.isJsonObject()) {
            JsonObject obj = input.getAsJsonObject();
            JsonObject processedObj = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String processedKey = escapeToUnicode(entry.getKey());
                JsonElement processedValue = processData(entry.getValue());
                processedObj.add(processedKey, processedValue);
            }
            return processedObj;
        } else if (input.isJsonArray()) {
            JsonArray array = input.getAsJsonArray();
            JsonArray processedArray = new JsonArray();
            for (JsonElement item : array) {
                processedArray.add(processData(item));
            }
            return processedArray;
        } else if (input.isJsonPrimitive()) {
            JsonPrimitive primitive = input.getAsJsonPrimitive();
            if (primitive.isString()) {
                return new JsonPrimitive(escapeToUnicode(primitive.getAsString()));
            }
        }
        return input;
    }

    private static boolean hasKey(JsonObject json, String key) {
        return json.has(key);
    }

    public static Set<ResourceKey> getAllResourceKeys(JsonObject jsonData) {
        Map<String, Function<JsonElement, Set<ResourceKey>>> handlerMap = getHandlerMap();
        Set<ResourceKey> resourceKeys = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (handlerMap.containsKey(key)) {
                resourceKeys.addAll(handlerMap.get(key).apply(value));
            } else if (value.isJsonObject()) {
                JsonObject valueObj = value.getAsJsonObject();
                if (hasKey(valueObj, "sounds")) {
                    resourceKeys.addAll(getSounds(valueObj));
                }
            }
        }
        return resourceKeys;
    }

    private static @NotNull Map<String, Function<JsonElement, Set<ResourceKey>>> getHandlerMap() {
        Map<String, Function<JsonElement, Set<ResourceKey>>> handlerMap = new HashMap<>();
        handlerMap.put("variants", JsonUtils::getVariants);
        handlerMap.put("multipart", JsonUtils::getMultipart);
        handlerMap.put("providers", JsonUtils::getProviders);
        handlerMap.put("model", JsonUtils::getModel);
        handlerMap.put("overrides", JsonUtils::getOverrides);
        handlerMap.put("parent", JsonUtils::getParent);
        handlerMap.put("textures", JsonUtils::getTextures);
        return handlerMap;
    }

    private static Set<ResourceKey> getSounds(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (data.isJsonObject()) {
            JsonObject jsonObject = data.getAsJsonObject();
            JsonArray sounds = jsonObject.get("sounds").getAsJsonArray();
            for (JsonElement sound : sounds) {
                if (sound.isJsonPrimitive() && sound.getAsJsonPrimitive().isString()) {
                    resourceKeys.add(ResourceKey.of(sound.getAsString(), ResourceType.SOUND));
                } else if (sound.isJsonObject()) {
                    if (hasKey(sound.getAsJsonObject(), "name")) {
                        resourceKeys.add(ResourceKey.of(sound.getAsJsonObject().get("name").getAsString(), ResourceType.SOUND));
                    }
                }
            }
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getVariants(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (data.isJsonObject()) {
            JsonObject jsonObject = data.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    JsonObject valueObj = value.getAsJsonObject();
                    if (hasKey(valueObj, "model")) {
                        JsonElement modelElement = valueObj.get("model");
                        if (modelElement.isJsonPrimitive()
                                && modelElement.getAsJsonPrimitive().isString()) {
                            resourceKeys.add(ResourceKey.of(
                                    modelElement.getAsString(),
                                    ResourceType.MODEL
                            ));
                        }
                    }
                }
                else if (value.isJsonArray()) {
                    JsonArray valueArray = value.getAsJsonArray();
                    for (JsonElement element : valueArray) {
                        if (element.isJsonObject()) {
                            JsonObject obj = element.getAsJsonObject();
                            if (hasKey(obj, "model")) {
                                JsonElement model = obj.get("model");
                                if (model.isJsonPrimitive()
                                        && model.getAsJsonPrimitive().isString()) {
                                    resourceKeys.add(ResourceKey.of(
                                            model.getAsString(),
                                            ResourceType.MODEL
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getMultipart(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (!data.isJsonArray()) return resourceKeys;
        for (JsonElement element : data.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject valueObj = element.getAsJsonObject();
            if (!hasKey(valueObj, "apply")) continue;
            JsonElement applyData = valueObj.get("apply");
            List<JsonElement> entries;
            if (applyData.isJsonArray()) {
                entries = applyData.getAsJsonArray().asList();
            } else if (applyData.isJsonObject()) {
                entries = Collections.singletonList(applyData);
            } else {
                continue;
            }
            for (JsonElement entryElement : entries) {
                if (!entryElement.isJsonObject()) continue;
                JsonObject entry = entryElement.getAsJsonObject();
                if (hasKey(entry, "model")) {
                    JsonElement modelElement = entry.get("model");
                    if (modelElement.isJsonPrimitive()
                            && modelElement.getAsJsonPrimitive().isString()) {
                        resourceKeys.add(ResourceKey.of(
                                modelElement.getAsString(),
                                ResourceType.MODEL
                        ));
                    }
                }
            }
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getProviders(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (!data.isJsonArray()) return resourceKeys;
        for (JsonElement element : data.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject valueObj = element.getAsJsonObject();
            if (hasKey(valueObj, "file") && valueObj.get("file").isJsonPrimitive()) {
                JsonElement fileElement = valueObj.get("file");
                if (fileElement.getAsJsonPrimitive().isString()) {
                    String fileStr = fileElement.getAsString();
                    if (fileStr.endsWith(".png")) {
                        String textureName = fileStr.substring(0, fileStr.length() - 4);
                        resourceKeys.add(ResourceKey.of(
                                textureName,
                                ResourceType.TEXTURE,
                                false,
                                true
                        ));
                    }
                }
            }
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getModel(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (data.isJsonObject()) {
            JsonObject jsonObject = data.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String dataKey = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    resourceKeys.addAll(getModel(value));
                }
                else if (value.isJsonArray()) {
                    for (JsonElement item : value.getAsJsonArray()) {
                        resourceKeys.addAll(getModel(item));
                    }
                }
                else if (dataKey.equals("model") && value.isJsonPrimitive()) {
                    if (value.getAsJsonPrimitive().isString()) {
                        String modelName = value.getAsString();
                        resourceKeys.add(ResourceKey.of(
                                modelName,
                                ResourceType.MODEL
                        ));
                    }
                }
            }
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getOverrides(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (!data.isJsonArray()) return resourceKeys;
        JsonArray dataArray = data.getAsJsonArray();
        for (JsonElement element : dataArray) {
            if (!element.isJsonObject()) continue;
            JsonObject valueObj = element.getAsJsonObject();
            if (hasKey(valueObj, "model")) {
                JsonElement modelElement = valueObj.get("model");
                if (modelElement.isJsonPrimitive()
                        && modelElement.getAsJsonPrimitive().isString()) {
                    String modelName = modelElement.getAsString();
                    resourceKeys.add(ResourceKey.of(
                            modelName,
                            ResourceType.MODEL
                    ));
                }
            }
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getParent(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) {
            String parentName = data.getAsString();
            resourceKeys.add(ResourceKey.of(parentName, ResourceType.MODEL));
        }
        return resourceKeys;
    }

    private static Set<ResourceKey> getTextures(JsonElement data) {
        Set<ResourceKey> resourceKeys = new HashSet<>();
        if (data.isJsonObject()) {
            JsonObject jsonObject = data.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    String textureName = value.getAsString();
                    resourceKeys.add(ResourceKey.of(
                            textureName,
                            ResourceType.TEXTURE
                    ));
                }
            }
        }
        else if (data.isJsonArray()) {
            JsonArray jsonArray = data.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String textureName = element.getAsString();
                    resourceKeys.add(ResourceKey.of(
                            textureName,
                            ResourceType.TEXTURE
                    ));
                }
            }
        }
        return resourceKeys;
    }

    public static JsonObject replaceResourceKey(JsonObject jsonData, Map<ResourceKey, ResourceKey> replaceMap) {
        Map<String, Consumer<JsonElement>> handlerMap = getHandlerMap(jsonData, replaceMap);
        for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (handlerMap.containsKey(key)) {
                handlerMap.get(key).accept(value);
            } else if (value.isJsonObject() && hasKey(value.getAsJsonObject(), "sounds")) {
                replaceSounds(value, replaceMap);
            }
        }
        return jsonData;
    }

    private static @NotNull Map<String, Consumer<JsonElement>> getHandlerMap(JsonObject jsonData, Map<ResourceKey, ResourceKey> replaceMap) {
        Map<String, Consumer<JsonElement>> handlerMap = new HashMap<>();
        handlerMap.put("variants", value -> replaceVariants(value, replaceMap));
        handlerMap.put("multipart", value -> replaceMultipart(value, replaceMap));
        handlerMap.put("providers", value -> replaceProviders(value, replaceMap));
        handlerMap.put("model", value -> replaceModel(value, replaceMap));
        handlerMap.put("overrides", value -> replaceOverrides(value, replaceMap));
        handlerMap.put("parent", value -> replaceParent(value, replaceMap, jsonData));
        handlerMap.put("textures", value -> replaceTextures(value, replaceMap));
        return handlerMap;
    }

    private static void replaceSounds(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (data.isJsonObject()) {
            JsonObject jsonObject = data.getAsJsonObject();
            JsonArray sounds = jsonObject.getAsJsonArray("sounds");
            for (int i = 0; i < sounds.size(); i++) {
                JsonElement sound = sounds.get(i);
                if (sound.isJsonPrimitive() && sound.getAsJsonPrimitive().isString()) {
                    String originalSound = sound.getAsString();
                    ResourceKey soundKey = ResourceKey.of(originalSound, ResourceType.SOUND);
                    if (replaceMap.containsKey(soundKey)) {
                        String newSound = replaceMap.get(soundKey).toString();
                        sounds.set(i, new JsonPrimitive(newSound));
                    }
                } else if (sound.isJsonObject()) {
                    JsonObject soundObj = sound.getAsJsonObject();
                    if (hasKey(soundObj, "name")) {
                        String originalName = soundObj.getAsJsonPrimitive("name").getAsString();
                        ResourceKey soundKey = ResourceKey.of(originalName, ResourceType.SOUND);
                        if (replaceMap.containsKey(soundKey)) {
                            String newName = replaceMap.get(soundKey).toString();
                            soundObj.addProperty("name", newName);
                        }
                    }
                }
            }
        }
    }

    private static void replaceVariants(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (data.isJsonObject()) {
            JsonObject dataObj = data.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : dataObj.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    replaceVariants(value, replaceMap);
                } else if (value.isJsonArray()) {
                    JsonArray valueArray = value.getAsJsonArray();
                    for (JsonElement element : valueArray) {
                        replaceVariants(element, replaceMap);
                    }
                } else if (value.isJsonPrimitive() && entry.getKey().equals("model")) {
                    ResourceKey keyModel = ResourceKey.of(value.getAsString(), ResourceType.MODEL);
                    if (replaceMap.containsKey(keyModel)) {
                        dataObj.addProperty("model", replaceMap.get(keyModel).toString());
                    }
                }
            }
        } else if (data.isJsonArray()) {
            JsonArray dataArray = data.getAsJsonArray();
            for (JsonElement element : dataArray) {
                replaceVariants(element, replaceMap);
            }
        } else if (data.isJsonObject()) {
            JsonObject dataObj = data.getAsJsonObject();
            if (hasKey(dataObj, "model")) {
                ResourceKey keyModel = ResourceKey.of(dataObj.get("model").getAsString(), ResourceType.MODEL);
                if (replaceMap.containsKey(keyModel)) {
                    dataObj.addProperty("model", replaceMap.get(keyModel).toString());
                }
            }
        }
    }

    private static void replaceMultipart(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (!data.isJsonArray()) return;
        JsonArray multipartArray = data.getAsJsonArray();
        for (JsonElement element : multipartArray) {
            if (!element.isJsonObject()) continue;
            JsonObject entryObj = element.getAsJsonObject();
            if (!hasKey(entryObj, "apply")) continue;
            JsonElement applyElement = entryObj.get("apply");
            JsonArray applyArray = new JsonArray();
            if (applyElement.isJsonArray()) {
                applyArray = applyElement.getAsJsonArray();
            } else if (applyElement.isJsonObject()) {
                applyArray.add(applyElement);
            }
            for (JsonElement applyEntry : applyArray) {
                if (!applyEntry.isJsonObject()) continue;
                JsonObject modelObj = applyEntry.getAsJsonObject();
                if (!hasKey(modelObj, "model")) continue;
                String modelValue = modelObj.get("model").getAsString();
                ResourceKey modelKey = ResourceKey.of(modelValue, ResourceType.MODEL);
                if (replaceMap.containsKey(modelKey)) {
                    modelObj.addProperty("model", replaceMap.get(modelKey).toString());
                }
            }
            if (applyElement.isJsonArray()) {
                entryObj.add("apply", applyArray);
            } else {
                entryObj.add("apply", applyArray.get(0));
            }
        }
    }

    private static void replaceProviders(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (!data.isJsonArray()) return;
        JsonArray providersArray = data.getAsJsonArray();
        for (JsonElement element : providersArray) {
            if (!element.isJsonObject()) continue;
            JsonObject providerObj = element.getAsJsonObject();
            if (!providerObj.has("file")) continue;
            String fileValue = providerObj.get("file").getAsString();
            if (fileValue.endsWith(".png")) {
                String textureKeyStr = fileValue.substring(0, fileValue.length() - 4);
                ResourceKey textureKey = ResourceKey.of(textureKeyStr, ResourceType.TEXTURE);
                if (replaceMap.containsKey(textureKey)) {
                    String newFileValue = replaceMap.get(textureKey).toString() + ".png";
                    providerObj.addProperty("file", newFileValue);
                }
            }
        }
    }

    private static void replaceModel(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (data.isJsonObject()) {
            JsonObject dataObj = data.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : dataObj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                if (value.isJsonObject()) {
                    replaceModel(value, replaceMap);
                } else if (value.isJsonArray()) {
                    JsonArray valueArray = value.getAsJsonArray();
                    for (JsonElement item : valueArray) {
                        replaceModel(item, replaceMap);
                    }
                } else if (value.isJsonPrimitive() && key.equals("model")) {
                    String modelValue = value.getAsString();
                    ResourceKey modelKey = ResourceKey.of(modelValue, ResourceType.MODEL);
                    if (replaceMap.containsKey(modelKey)) {
                        dataObj.addProperty("model", replaceMap.get(modelKey).toString());
                    }
                }
            }
        } else if (data.isJsonArray()) {
            JsonArray dataArray = data.getAsJsonArray();
            for (JsonElement item : dataArray) {
                replaceModel(item, replaceMap);
            }
        }
    }

    private static void replaceOverrides(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (!data.isJsonArray()) return;
        JsonArray overridesArray = data.getAsJsonArray();
        for (JsonElement element : overridesArray) {
            if (!element.isJsonObject()) continue;
            JsonObject overrideObj = element.getAsJsonObject();
            if (!overrideObj.has("model")) continue;
            String modelValue = overrideObj.get("model").getAsString();
            ResourceKey modelKey = ResourceKey.of(modelValue, ResourceType.MODEL);
            if (replaceMap.containsKey(modelKey)) {
                overrideObj.addProperty("model", replaceMap.get(modelKey).toString());
            }
        }
    }

    private static void replaceParent(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap, JsonObject jsonData) {
        if (data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) {
            ResourceKey parentKey = ResourceKey.of(data.getAsString(), ResourceType.MODEL);
            if (replaceMap.containsKey(parentKey)) {
                jsonData.addProperty("parent", replaceMap.get(parentKey).toString());
            }
        }
    }

    private static void replaceTextures(JsonElement data, Map<ResourceKey, ResourceKey> replaceMap) {
        if (data.isJsonObject()) {
            JsonObject texturesObj = data.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : texturesObj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    String textureValue = value.getAsString();
                    ResourceKey textureKey = ResourceKey.of(textureValue, ResourceType.TEXTURE);
                    if (replaceMap.containsKey(textureKey)) {
                        texturesObj.addProperty(key, replaceMap.get(textureKey).toString());
                    }
                }
            }
        } else if (data.isJsonArray()) {
            JsonArray texturesArray = data.getAsJsonArray();
            for (int i = 0; i < texturesArray.size(); i++) {
                JsonElement element = texturesArray.get(i);
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String textureValue = element.getAsString();
                    ResourceKey textureKey = ResourceKey.of(textureValue, ResourceType.TEXTURE);
                    if (replaceMap.containsKey(textureKey)) {
                        texturesArray.set(i, new JsonPrimitive(replaceMap.get(textureKey).toString()));
                    }
                }
            }
        }
    }
}
