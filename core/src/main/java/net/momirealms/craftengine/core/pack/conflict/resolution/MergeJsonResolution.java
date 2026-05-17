package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;

public record MergeJsonResolution(boolean deeply) implements Resolution {
    public static final ResolutionFactory<MergeJsonResolution> FACTORY = new Factory();

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            JsonObject j1 = GsonHelper.readJsonFromFile(existing.path()).getAsJsonObject();
            JsonObject j2 = GsonHelper.readJsonFromFile(conflict.path()).getAsJsonObject();
            JsonObject j3;
            if (deeply) {
                j3 = GsonHelper.deepMerge(j1, j2);
            } else {
                j3 = GsonHelper.shallowMerge(j1, j2);
            }
            GsonHelper.writeJsonFile(j3, existing.path());
        } catch (IOException e) {
            CraftEngine.instance().logger().error("Failed to merge json when resolving file conflicts", e);
        }
    }

    private static class Factory implements ResolutionFactory<MergeJsonResolution> {

        @Override
        public MergeJsonResolution create(ConfigSection section) {
            return new MergeJsonResolution(section.getBoolean("deeply"));
        }
    }
}
