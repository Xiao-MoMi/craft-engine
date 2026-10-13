package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.util.ResourcePackJsonUtils;

import java.nio.file.Path;

record ValidationContext(PluginLogger logger, ValidationResources resources, ValidationSettings settings) {

    JsonObject readJson(Path path) {
        return ResourcePackJsonUtils.readJsonObjectOrWarn(this.logger, path);
    }

    void writeJson(JsonElement json, Path path) {
        ResourcePackJsonUtils.writeJsonSafely(json, path);
    }
}
