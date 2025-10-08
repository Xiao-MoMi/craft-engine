package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.model.LegacyOverridesModel;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

public class ResolutionMergeLegacyModel implements Resolution {
    public static final Factory FACTORY = new Factory();

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            JsonObject j1 = GsonHelper.readJsonFile(existing.path()).getAsJsonObject();
            JsonObject j2 = GsonHelper.readJsonFile(conflict.path()).getAsJsonObject();

            if (!isJsonArray(j2.get("overrides"))) {
                return;
            }

            if (!isJsonArray(j1.get("overrides"))) {
                GsonHelper.writeJsonFile(j2, existing.path());
                return;
            }

            JsonArray ja1 = j1.getAsJsonArray("overrides");
            JsonArray ja2 = j2.getAsJsonArray("overrides");
            TreeSet<LegacyOverridesModel> legacyOverridesModels = new TreeSet<>();
            for (JsonElement je : ja1) {
                if (je instanceof JsonObject jo) {
                    legacyOverridesModels.add(new LegacyOverridesModel(jo));
                }
            }
            for (JsonElement je : ja2) {
                if (je instanceof JsonObject jo) {
                    legacyOverridesModels.add(new LegacyOverridesModel(jo));
                }
            }

            JsonArray newOverrides = new JsonArray();
            for (LegacyOverridesModel legacyOverridesModel : legacyOverridesModels) {
                newOverrides.add(legacyOverridesModel.toLegacyPredicateElement());
            }

            j2.add("overrides", newOverrides);
            GsonHelper.writeJsonFile(j2, existing.path());
        } catch (IOException e) {
            CraftEngine.instance().logger().severe("Failed to merge json when resolving file conflicts", e);
        }
    }

    private boolean isJsonArray(JsonElement element) {
        return element != null && element.isJsonArray();
    }

    @Override
    public Key type() {
        return Resolutions.MERGE_LEGACY_MODEL;
    }

    public static class Factory implements ResolutionFactory {

        @Override
        public Resolution create(Map<String, Object> arguments) {
            return new ResolutionMergeLegacyModel();
        }
    }
}
