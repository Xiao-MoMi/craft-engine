package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyOverridesModel;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.util.TreeSet;

public final class MergeLegacyModelResolution implements Resolution {
    public static final ResolutionFactory<MergeLegacyModelResolution> FACTORY = new Factory();
    public static final MergeLegacyModelResolution INSTANCE = new MergeLegacyModelResolution();

    private MergeLegacyModelResolution() {}

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            JsonObject j1 = GsonHelper.readJsonFromFile(existing.path()).getAsJsonObject();
            JsonObject j2 = GsonHelper.readJsonFromFile(conflict.path()).getAsJsonObject();

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
            CraftEngine.instance().logger().error("Failed to merge json when resolving file conflicts", e);
        }
    }

    private boolean isJsonArray(JsonElement element) {
        return element != null && element.isJsonArray();
    }

    private static class Factory implements ResolutionFactory<MergeLegacyModelResolution> {

        @Override
        public MergeLegacyModelResolution create(ConfigSection section) {
            return INSTANCE;
        }
    }
}
