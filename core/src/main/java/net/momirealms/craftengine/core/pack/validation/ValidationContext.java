package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.ResourcePackJsonUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

record ValidationContext(ValidationResources resources, ValidationSettings settings, Map<Path, JsonObject> modelDocuments) {

    ValidationContext(ValidationResources resources, ValidationSettings settings) {
        this(resources, settings, new HashMap<>());
    }

    JsonObject readJson(Path path) {
        return ResourcePackJsonUtils.readJsonObjectOrWarn(path);
    }

    JsonObject readModel(Path path) {
        Path key = path.normalize();
        JsonObject model = this.modelDocuments.get(key);
        if (model == null) {
            model = this.readJson(path);
            if (model != null) {
                this.modelDocuments.put(key, model);
            }
        }
        return model;
    }

    void writeJson(JsonElement json, Path path) {
        // 模型修复会直接修改 JSON，后续区间应读取实际写出的文件
        this.modelDocuments.remove(path.normalize());
        ResourcePackJsonUtils.writeJsonSafely(json, path);
    }
}
