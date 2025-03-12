package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class MergeJsonResolution implements Resolution {
    public static final Factory FACTORY = new Factory();
    private final boolean deeply;

    public MergeJsonResolution(boolean deeply) {
        this.deeply = deeply;
    }

    @Override
    public void run(Path existing, Path conflict) {
        try {
            JsonObject j1 = GsonHelper.readJsonFile(existing).getAsJsonObject();
            JsonObject j2 = GsonHelper.readJsonFile(conflict).getAsJsonObject();
            JsonObject j3;
            if (deeply) {
                j3 = GsonHelper.deepMerge(j1, j2);
            } else {
                j3 = GsonHelper.shallowMerge(j1, j2);
            }
            GsonHelper.writeJsonFile(j3, existing);
        } catch (IOException e) {
            CraftEngine.instance().logger().severe("Failed to merge json when resolving file conflicts", e);
        }
    }

    @Override
    public Key type() {
        return Resolutions.MERGE_JSON;
    }

    public static class Factory implements ResolutionFactory {

        @Override
        public Resolution create(Map<String, Object> arguments) {
            boolean deeply = (boolean) arguments.getOrDefault("deeply", false);
            return new MergeJsonResolution(deeply);
        }
    }
}
