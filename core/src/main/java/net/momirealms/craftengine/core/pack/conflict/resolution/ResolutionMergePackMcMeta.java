package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.*;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ResolutionMergePackMcMeta implements Resolution {
    public static final Factory FACTORY = new Factory();
    private final String description;

    public ResolutionMergePackMcMeta(String description) {
        this.description = description;
    }

    record MinMax(int min, int max) {
    }

    @SuppressWarnings("DuplicatedCode")
    public static void mergeMcMeta(Path file1, Path file2, JsonElement customDescription) throws IOException {
        JsonObject mcmeta1 = GsonHelper.readJsonFile(file1).getAsJsonObject();
        JsonObject mcmeta2 = GsonHelper.readJsonFile(file2).getAsJsonObject();

        JsonObject merged = mergeValues(mcmeta1, mcmeta2).getAsJsonObject();

        if (mcmeta1.has("pack") && mcmeta2.has("pack")) {
            JsonObject mergedPack = merged.getAsJsonObject("pack");
            JsonObject mcmeta1Pack = mcmeta1.getAsJsonObject("pack");
            JsonObject mcmeta2Pack = mcmeta2.getAsJsonObject("pack");

            int minPackFormat = Integer.MAX_VALUE;
            int maxPackFormat = Integer.MIN_VALUE;
            JsonArray mergedMinFormat = null;
            JsonArray mergedMaxFormat = null;

            if (mcmeta1Pack.has("pack_format") && mcmeta2Pack.has("pack_format")) {
                int packFormat1 = mcmeta1Pack.getAsJsonPrimitive("pack_format").getAsInt();
                int packFormat2 = mcmeta2Pack.getAsJsonPrimitive("pack_format").getAsInt();
                int mergedPackFormat = maxPackFormat = Math.max(packFormat1, packFormat2);
                minPackFormat = Math.min(packFormat1, packFormat2);
                mergedPack.addProperty("pack_format", mergedPackFormat);
            } else if (mcmeta1Pack.has("pack_format")) {
                minPackFormat = maxPackFormat = mcmeta1Pack.getAsJsonPrimitive("pack_format").getAsInt();
            } else if (mcmeta2Pack.has("pack_format")) {
                minPackFormat = maxPackFormat = mcmeta2Pack.getAsJsonPrimitive("pack_format").getAsInt();
            }

            if (mcmeta1Pack.has("min_format") || mcmeta2Pack.has("min_format")) {
                int[] minFormat1 = new int[]{Integer.MAX_VALUE, 0};
                int[] minFormat2 = new int[]{Integer.MAX_VALUE, 0};

                if (mcmeta1Pack.has("min_format")) {
                    JsonElement minFormat = mcmeta1Pack.get("min_format");
                    if (minFormat.isJsonPrimitive()) {
                        minFormat1[0] = minFormat.getAsInt();
                    }
                    if (minFormat.isJsonArray()) {
                        JsonArray minFormatArray = minFormat.getAsJsonArray();
                        minFormat1[0] = minFormatArray.get(0).getAsInt();
                        if (minFormatArray.size() > 1) {
                            minFormat1[1] = minFormatArray.get(1).getAsInt();
                        }
                    }
                }

                if (mcmeta2Pack.has("min_format")) {
                    JsonElement minFormat = mcmeta2Pack.get("min_format");
                    if (minFormat.isJsonPrimitive()) {
                        minFormat2[0] = minFormat.getAsInt();
                    }
                    if (mcmeta2Pack.isJsonArray()) {
                        JsonArray minFormatArray = minFormat.getAsJsonArray();
                        minFormat2[0] = minFormatArray.get(0).getAsInt();
                        if (minFormatArray.size() > 1) {
                            minFormat2[1] = minFormatArray.get(1).getAsInt();
                        }
                    }
                }
                minPackFormat = Math.min(minPackFormat, Math.min(minFormat1[0], minFormat2[0]));
                mergedMinFormat = new JsonArray(2);
                mergedMinFormat.add(minPackFormat);
                mergedMinFormat.add(Math.min(minFormat1[1], minFormat2[1]));
                mergedPack.add("min_format", mergedMinFormat);
            }

            if (mcmeta1Pack.has("max_format") || mcmeta2Pack.has("max_format")) {
                int[] maxFormat1 = new int[]{Integer.MIN_VALUE, 0};
                int[] maxFormat2 = new int[]{Integer.MIN_VALUE, 0};

                if (mcmeta1Pack.has("max_format")) {
                    JsonElement maxFormat = mcmeta1Pack.get("max_format");
                    if (maxFormat.isJsonPrimitive()) {
                        maxFormat1[0] = maxFormat.getAsInt();
                    }
                    if (maxFormat.isJsonArray()) {
                        JsonArray maxFormatArray = maxFormat.getAsJsonArray();
                        maxFormat1[0] = maxFormatArray.get(0).getAsInt();
                        if (maxFormatArray.size() > 1) {
                            maxFormat1[1] = maxFormatArray.get(1).getAsInt();
                        }
                    }
                }

                if (mcmeta2Pack.has("max_format")) {
                    JsonElement maxFormat = mcmeta2Pack.get("max_format");
                    if (maxFormat.isJsonPrimitive()) {
                        maxFormat2[0] = maxFormat.getAsInt();
                    }
                    if (maxFormat.isJsonArray()) {
                        JsonArray maxFormatArray = maxFormat.getAsJsonArray();
                        maxFormat2[0] = maxFormatArray.get(0).getAsInt();
                        if (maxFormatArray.size() > 1) {
                            maxFormat2[1] = maxFormatArray.get(1).getAsInt();
                        }
                    }
                }

                maxPackFormat = Math.max(maxPackFormat, Math.max(maxFormat1[0], maxFormat2[0]));
                mergedMaxFormat = new JsonArray(2);
                mergedMaxFormat.add(maxPackFormat);
                mergedMaxFormat.add(Math.max(maxFormat1[1], maxFormat2[1]));
                mergedPack.add("max_format", mergedMaxFormat);
            }

            JsonElement supportedFormats1 = mcmeta1Pack.get("supported_formats");
            JsonElement supportedFormats2 = mcmeta2Pack.get("supported_formats");

            if (supportedFormats1 != null || supportedFormats2 != null) {
                MinMax mergedMinMax = getMergedMinMax(supportedFormats1, supportedFormats2, minPackFormat, maxPackFormat);
                JsonElement mergedSupportedFormats = createSupportedFormatsElement(
                        supportedFormats1 != null ? supportedFormats1 : supportedFormats2,
                        mergedMinMax.min,
                        mergedMinMax.max
                );
                if (mergedMinFormat != null && !mergedMinFormat.isEmpty()) {
                    mergedMinFormat.set(0, new JsonPrimitive(Math.min(mergedMinMax.min, mergedMinFormat.get(0).getAsInt())));
                }
                if (mergedMaxFormat != null && !mergedMaxFormat.isEmpty()) {
                    mergedMaxFormat.set(0, new JsonPrimitive(Math.max(mergedMinMax.max, mergedMaxFormat.get(0).getAsInt())));
                }
                mergedPack.add("supported_formats", mergedSupportedFormats);
            }

            if (customDescription != null) {
                mergedPack.add("description", customDescription);
            } else {
                JsonPrimitive description1 = mcmeta1.getAsJsonObject().getAsJsonObject("pack")
                        .getAsJsonPrimitive("description");
                JsonPrimitive description2 = mcmeta2.getAsJsonObject().getAsJsonObject("pack")
                        .getAsJsonPrimitive("description");

                String mergedDesc = (description1 != null ? description1.getAsString() : "")
                        + (description1 != null && description2 != null ? "\n" : "")
                        + (description2 != null ? description2.getAsString() : "");

                if (!mergedDesc.isEmpty()) {
                    mergedPack.addProperty("description", mergedDesc);
                }
            }
        }

        GsonHelper.writeJsonFile(merged, file1);
    }

    private static MinMax getMergedMinMax(JsonElement sf1, JsonElement sf2, int minPackFormat, int maxPackFormat) {
        MinMax mm1 = parseSupportedFormats(sf1);
        MinMax mm2 = parseSupportedFormats(sf2);

        int finalMin = Math.min(mm1.min, mm2.min);
        int finalMax = Math.max(mm1.max, mm2.max);
        finalMin = Math.min(minPackFormat, finalMin);
        finalMax = Math.max(maxPackFormat, finalMax);

        return new MinMax(finalMin, finalMax);
    }

    private static MinMax parseSupportedFormats(JsonElement supported) {
        if (supported == null || supported.isJsonNull()) {
            return new MinMax(Integer.MAX_VALUE, Integer.MIN_VALUE);
        }

        if (supported.isJsonPrimitive()) {
            int value = supported.getAsInt();
            return new MinMax(value, value);
        }

        if (supported.isJsonArray()) {
            JsonArray arr = supported.getAsJsonArray();
            int min = arr.get(0).getAsInt();
            int max = arr.get(arr.size() - 1).getAsInt();
            return new MinMax(min, max);
        }

        JsonObject obj = supported.getAsJsonObject();
        int min, max;

        if (obj.has("min_inclusive")) {
            min = obj.get("min_inclusive").getAsInt();
        } else if (obj.has("max_inclusive")) {
            min = obj.get("max_inclusive").getAsInt();
        } else {
            throw new IllegalArgumentException("Invalid supported_formats format");
        }

        if (obj.has("max_inclusive")) {
            max = obj.get("max_inclusive").getAsInt();
        } else {
            max = min;
        }

        return new MinMax(min, max);
    }

    private static JsonElement createSupportedFormatsElement(
            JsonElement originalFormat,
            int min,
            int max) {

        if (originalFormat.isJsonPrimitive()) {
            return new JsonPrimitive(Math.max(min, max));

        } else if (originalFormat.isJsonArray()) {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive(min));
            array.add(new JsonPrimitive(max));
            return array;

        } else if (originalFormat.isJsonObject()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("min_inclusive", min);
            obj.addProperty("max_inclusive", max);
            return obj;
        }

        return JsonNull.INSTANCE;
    }

    private static JsonElement mergeValues(JsonElement v1, JsonElement v2) {
        if (v1.isJsonObject() && v2.isJsonObject()) {
            JsonObject obj1 = v1.getAsJsonObject();
            JsonObject obj2 = v2.getAsJsonObject();
            JsonObject merged = new JsonObject();

            for (String key : obj1.keySet()) {
                if (obj2.has(key)) {
                    merged.add(key, mergeValues(obj1.get(key), obj2.get(key)));
                } else {
                    merged.add(key, obj1.get(key));
                }
            }
            for (String key : obj2.keySet()) {
                if (!merged.has(key)) {
                    merged.add(key, obj2.get(key));
                }
            }
            return merged;
        }

        if (v1.isJsonArray() && v2.isJsonArray()) {
            JsonArray arr1 = v1.getAsJsonArray();
            JsonArray arr2 = v2.getAsJsonArray();
            JsonArray merged = new JsonArray();
            merged.addAll(arr2);
            merged.addAll(arr1);
            return merged;
        }

        return v2.isJsonNull() ? v1 : v2;
    }

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            mergeMcMeta(existing.path(), conflict.path(), AdventureHelper.componentToJsonElement(AdventureHelper.miniMessage().deserialize(this.description)));
        } catch (IOException e) {
            CraftEngine.instance().logger().severe("Failed to merge pack.mcmeta when resolving file conflicts", e);
        }
    }

    @Override
    public Key type() {
        return Resolutions.MERGE_PACK_MCMETA;
    }

    public static class Factory implements ResolutionFactory {
        @Override
        public Resolution create(Map<String, Object> arguments) {
            String description = arguments.getOrDefault("description", "<gray>CraftEngine ResourcePack</gray>").toString();
            return new ResolutionMergePackMcMeta(description);
        }
    }
}
