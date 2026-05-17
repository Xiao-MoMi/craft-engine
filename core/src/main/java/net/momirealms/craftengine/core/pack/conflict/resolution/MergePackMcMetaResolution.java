package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public final class MergePackMcMetaResolution implements Resolution {
    public static final ResolutionFactory<MergePackMcMetaResolution> FACTORY = new Factory();
    public static final Set<String> STANDARD_PACK_KEYS = ImmutableSet.of("pack", "features", "filter", "overlays", "language");
    public static final MergePackMcMetaResolution INSTANCE = new MergePackMcMetaResolution();

    private MergePackMcMetaResolution() {}

    public static void merge(Path file1, Path file2) throws IOException {
        // 第一步，解析全部的mcmeta文件为json对象
        JsonObject mcmeta1;
        try {
            mcmeta1 = GsonHelper.readJsonFromFile(file1).getAsJsonObject();
        } catch (Exception e) {
            CraftEngine.instance().logger().error("Failed to parse mcmeta from " + file1);
            return;
        }
        JsonObject mcmeta2;
        try {
            mcmeta2 = GsonHelper.readJsonFromFile(file2).getAsJsonObject();
        } catch (Exception e) {
            CraftEngine.instance().logger().error("Failed to parse mcmeta from " + file2);
            return;
        }
        JsonObject merged = new JsonObject();

// 注释: 无需处理，由后续验证合并
//        //第二步，处理pack区域
//        JsonObject pack1 = mcmeta1.getAsJsonObject("pack");
//        JsonObject pack2 = mcmeta2.getAsJsonObject("pack");
//        JsonObject mergedPack = new JsonObject();
//        merged.add("pack", mergedPack);
//        mergePack(mergedPack, pack1, pack2);

        // 第三步，合并overlays
        List<JsonObject> overlays = new ArrayList<>();
        collectOverlays(mcmeta1.getAsJsonObject("overlays"), overlays::add);
        collectOverlays(mcmeta2.getAsJsonObject("overlays"), overlays::add);
        if (!overlays.isEmpty()) {
            Map<String, JsonObject> overlayMap = new LinkedHashMap<>();
            for (JsonObject overlay : overlays) {
                JsonPrimitive directory = overlay.getAsJsonPrimitive("directory");
                if (directory != null) {
                    overlayMap.merge(directory.getAsString(), overlay, MergePackMcMetaResolution::combineOverlays);
                }
            }
            if (!overlayMap.isEmpty()) {
                JsonObject mergedOverlay = new JsonObject();
                JsonArray entries = new JsonArray();
                for (JsonObject entry : overlayMap.values()) {
                    entries.add(entry);
                }
                mergedOverlay.add("entries", entries);
                merged.add("overlays", mergedOverlay);
            }
        }
        // 第四步，合并filter
        List<JsonObject> filters = new ArrayList<>();
        collectFilters(mcmeta1.getAsJsonObject("filter"), filters::add);
        collectFilters(mcmeta2.getAsJsonObject("filter"), filters::add);
        if (!filters.isEmpty()) {
            JsonObject mergedFilter = new JsonObject();
            JsonArray blocks = new JsonArray();
            for (JsonObject entry : filters) {
                blocks.add(entry);
            }
            mergedFilter.add("block", blocks);
            merged.add("filter", mergedFilter);
        }
        // 第五步，合并features
        JsonArray enabledFeatures = new JsonArray();
        getOptionalFeatures(mcmeta1.getAsJsonObject("features")).ifPresent(enabledFeatures::addAll);
        getOptionalFeatures(mcmeta2.getAsJsonObject("features")).ifPresent(enabledFeatures::addAll);
        if (!enabledFeatures.isEmpty()) {
            JsonObject features = new JsonObject();
            features.add("enabled", enabledFeatures);
            merged.add("features", features);
        }
        // 第六步，合并language
        JsonObject newLanguage = new JsonObject();
        getOptionalLanguage(mcmeta1.getAsJsonObject("language")).ifPresent(it -> {
            for (Map.Entry<String, JsonElement> entry : it.entrySet()) {
                newLanguage.add(entry.getKey(), entry.getValue());
            }
        });
        getOptionalLanguage(mcmeta2.getAsJsonObject("language")).ifPresent(it -> {
            for (Map.Entry<String, JsonElement> entry : it.entrySet()) {
                newLanguage.add(entry.getKey(), entry.getValue());
            }
        });
        if (!newLanguage.asMap().isEmpty()) { // 兼容低版本gson
            merged.add("language", newLanguage);
        }
        // 第七步，合并其他未知元素
        for (Map.Entry<String, JsonElement> entry : mcmeta1.entrySet()) {
            if (!STANDARD_PACK_KEYS.contains(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, JsonElement> entry : mcmeta2.entrySet()) {
            if (!STANDARD_PACK_KEYS.contains(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue());
            }
        }
        // 第八步，写入
        GsonHelper.writeJsonFile(merged, file1);
    }

    private static Optional<JsonObject> getOptionalLanguage(JsonObject language) {
        if (language == null) return Optional.empty();
        return Optional.of(language);
    }

    private static Optional<JsonArray> getOptionalFeatures(JsonObject feature) {
        if (feature == null) return Optional.empty();
        return Optional.ofNullable(feature.getAsJsonArray("enabled"));
    }

    private static void collectFilters(JsonObject filterJson, Consumer<JsonObject> overlayCollector) {
        if (filterJson == null) return;
        JsonArray entries = filterJson.getAsJsonArray("block");
        if (entries == null) return;
        for (JsonElement entry : entries) {
            if (entry.isJsonObject()) {
                JsonObject entryJson = entry.getAsJsonObject();
                if (entryJson == null) continue;
                overlayCollector.accept(entryJson);
            }
        }
    }

    private static JsonObject combineOverlays(JsonObject overlay1, JsonObject overlay2) {
        Pair<PackVersion, PackVersion> v1 = getOverlayVersions(overlay1);
        Pair<PackVersion, PackVersion> v2 = getOverlayVersions(overlay2);
        PackVersion min = PackVersion.getLower(v1.left(), v2.left());
        PackVersion max = PackVersion.getHigher(v1.right(), v2.right());
        JsonObject merged = new JsonObject();
        merged.add("directory", overlay1.getAsJsonPrimitive("directory"));
        // 旧版格式支持
        JsonArray supportedFormats = new JsonArray();
        supportedFormats.add(min.major());
        supportedFormats.add(max.major());
        merged.add("formats", supportedFormats);
        // 新版格式支持
        JsonArray minFormat = new JsonArray();
        minFormat.add(min.major());
        minFormat.add(min.minor());
        merged.add("min_format", minFormat);
        JsonArray maxFormat = new JsonArray();
        maxFormat.add(max.major());
        maxFormat.add(max.minor());
        merged.add("max_format", maxFormat);
        return merged;
    }

    private static void collectOverlays(JsonObject overlayJson, Consumer<JsonObject> overlayCollector) {
        if (overlayJson == null) return;
        JsonArray entries = overlayJson.getAsJsonArray("entries");
        if (entries == null) return;
        for (JsonElement entry : entries) {
            if (entry.isJsonObject()) {
                JsonObject entryJson = entry.getAsJsonObject();
                if (entryJson == null) continue;
                Pair<PackVersion, PackVersion> supportedVersions = getOverlayVersions(entryJson);
                PackVersion min = PackVersion.getHigher(supportedVersions.left(), PackVersion.MIN_OVERLAY_VERSION);
                PackVersion max = PackVersion.getHigher(supportedVersions.right(), PackVersion.MIN_OVERLAY_VERSION);
                // 旧版格式支持
                JsonArray supportedFormats = new JsonArray();
                supportedFormats.add(min.major());
                supportedFormats.add(max.major());
                entryJson.add("formats", supportedFormats);
                // 新版格式支持
                JsonArray minFormat = new JsonArray();
                minFormat.add(min.major());
                minFormat.add(min.minor());
                entryJson.add("min_format", minFormat);
                JsonArray maxFormat = new JsonArray();
                maxFormat.add(max.major());
                maxFormat.add(max.minor());
                entryJson.add("max_format", maxFormat);
                overlayCollector.accept(entryJson);
            }
        }
    }

    private static Pair<PackVersion, PackVersion> getOverlayVersions(JsonObject pack) {
        if (pack == null) return Pair.of(PackVersion.MIN_OVERLAY_VERSION, PackVersion.MAX_PACK_VERSION);
        List<PackVersion> minVersions = new ArrayList<>();
        List<PackVersion> maxVersions = new ArrayList<>();
        if (pack.has("min_format")) {
            minVersions.add(getFormatVersion(pack.get("min_format"), PackVersion.MIN_OVERLAY_VERSION));
        }
        if (pack.has("max_format")) {
            maxVersions.add(getFormatVersion(pack.get("max_format"), PackVersion.MAX_PACK_VERSION));
        }
        if (pack.has("formats")) {
            Pair<PackVersion, PackVersion> supportedFormats = parseSupportedFormats(pack.get("formats"));
            minVersions.add(supportedFormats.left());
            maxVersions.add(supportedFormats.right());
        }
        if (maxVersions.isEmpty()) {
            maxVersions.add(PackVersion.MAX_PACK_VERSION);
        }
        if (minVersions.isEmpty()) {
            minVersions.add(PackVersion.MIN_OVERLAY_VERSION);
        }
        return Pair.of(
                PackVersion.getLowest(minVersions),
                PackVersion.getHighest(maxVersions)
        );
    }

    private static Pair<PackVersion, PackVersion> parseSupportedFormats(JsonElement formats) {
        switch (formats) {
            case null -> {
                return Pair.of(PackVersion.MIN_OVERLAY_VERSION, PackVersion.MAX_PACK_VERSION);
            }
            case JsonPrimitive jsonPrimitive -> {
                return new Pair<>(new PackVersion(jsonPrimitive.getAsInt(), 0), new PackVersion(jsonPrimitive.getAsInt(), 0));
            }
            case JsonArray array -> {
                if (array.isEmpty()) return Pair.of(PackVersion.MIN_OVERLAY_VERSION, PackVersion.MAX_PACK_VERSION);
                if (array.size() == 1) {
                    return new Pair<>(new PackVersion(GsonHelper.getAsInt(array.get(0), PackVersion.MIN_OVERLAY_VERSION.major()), 0), PackVersion.MAX_PACK_VERSION);
                }
                if (array.size() == 2) {
                    return new Pair<>(new PackVersion(GsonHelper.getAsInt(array.get(0), PackVersion.MIN_OVERLAY_VERSION.major()), 0), new PackVersion(GsonHelper.getAsInt(array.get(1), PackVersion.MAX_PACK_VERSION.major()), 0));
                }
            }
            case JsonObject object -> {
                int min = GsonHelper.getAsInt(object.get("min_inclusive"), PackVersion.MIN_OVERLAY_VERSION.major());
                int max = GsonHelper.getAsInt(object.get("max_inclusive"), PackVersion.MAX_PACK_VERSION.major());
                return new Pair<>(new PackVersion(min, 0), new PackVersion(max, 0));
            }
            default -> {
            }
        }
        return Pair.of(PackVersion.MIN_OVERLAY_VERSION, PackVersion.MAX_PACK_VERSION);
    }

    private static PackVersion getFormatVersion(JsonElement format, PackVersion defaultVersion) {
        if (format instanceof JsonArray array) {
            if (array.isEmpty()) return defaultVersion;
            if (array.size() == 1) {
                return new PackVersion(GsonHelper.getAsInt(array.get(0), defaultVersion.major()), 0);
            }
            if (array.size() == 2) {
                return new PackVersion(GsonHelper.getAsInt(array.get(0), defaultVersion.major()), GsonHelper.getAsInt(array.get(1), defaultVersion.minor()));
            }
        } else if (format instanceof JsonPrimitive jsonPrimitive) {
            float version = jsonPrimitive.getAsFloat();
            return PackVersion.parse(version);
        }
        return defaultVersion;
    }

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            merge(existing.path(), conflict.path());
        } catch (Exception e) {
            CraftEngine.instance().logger().error("Failed to merge pack.mcmeta when resolving file conflicts for '" + existing.path()  + "' and '" + conflict.path() + "'", e);
        }
    }

    private static class Factory implements ResolutionFactory<MergePackMcMetaResolution> {
        @Override
        public MergePackMcMetaResolution create(ConfigSection section) {
            return INSTANCE;
        }
    }
}
