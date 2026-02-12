package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.util.*;

public final class MergeAltasResolution implements Resolution {
    public static final ResolutionFactory<MergeAltasResolution> FACTORY = new Factory();
    public static final MergeAltasResolution INSTANCE = new MergeAltasResolution();

    private MergeAltasResolution() {}

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            JsonObject j1 = GsonHelper.readJsonFile(existing.path()).getAsJsonObject();
            JsonObject j2 = GsonHelper.readJsonFile(conflict.path()).getAsJsonObject();
            JsonObject j3 = new JsonObject();
            JsonArray ja1 = j1.getAsJsonArray("sources");
            JsonArray ja2 = j2.getAsJsonArray("sources");
            JsonArray ja3 = new JsonArray();
            HashSet<String> elements = new HashSet<>();
            for (JsonElement je : ja1) {
                if (je instanceof JsonObject jo) {
                    if (elements.add(normalizeWithSortedKeys(jo).toString())) {
                        ja3.add(jo);
                    }
                }
            }
            for (JsonElement je : ja2) {
                if (je instanceof JsonObject jo) {
                    if (elements.add(normalizeWithSortedKeys(jo).toString())) {
                        ja3.add(jo);
                    }
                }
            }
            j3.add("sources", ja3);
            GsonHelper.writeJsonFile(j3, existing.path());
        } catch (Exception e) {
            CraftEngine.instance().logger().severe("Failed to merge altas when resolving file conflicts", e);
        }
    }

    // 不完全对，因为还有array未排序
    private JsonObject normalizeWithSortedKeys(JsonObject obj) {
        // 收集所有键并排序
        List<String> keys = new ArrayList<>(obj.keySet());
        Collections.sort(keys);

        // 按排序后的键构建规范化的JSON
        JsonObject sortedObj = new JsonObject();
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element instanceof JsonObject innerJo) {
                sortedObj.add(key, normalizeWithSortedKeys(innerJo));
            } else {
                sortedObj.add(key, element);
            }
        }

        return sortedObj;
    }

    private static class Factory implements ResolutionFactory<MergeAltasResolution> {

        @Override
        public MergeAltasResolution create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
