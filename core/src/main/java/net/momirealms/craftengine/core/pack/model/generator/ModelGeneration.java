package net.momirealms.craftengine.core.pack.model.generator;

import com.google.gson.JsonObject;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ModelGeneration {
    private final Key path;
    private final String parentModelPath;
    private final Map<String, String> texturesOverride;

    public ModelGeneration(Key path, String parentModelPath, Map<String, String> texturesOverride) {
        this.path = path;
        this.parentModelPath = parentModelPath;
        this.texturesOverride = texturesOverride;
    }

    public ModelGeneration(Key path, Section section) {
        this.path = path;
        this.parentModelPath = Objects.requireNonNull(section.getString("parent"));
        Section texturesSection = section.getSection("textures");
        if (texturesSection != null) {
            this.texturesOverride = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : texturesSection.getStringRouteMappedValues(false).entrySet()) {
                if (entry.getValue() instanceof String p) {
                    this.texturesOverride.put(entry.getKey(), p);
                }
            }
        } else {
            this.texturesOverride = Collections.emptyMap();
        }
    }

    public ModelGeneration(Key path, Map<String, Object> map) {
        this.path = path;
        this.parentModelPath = Objects.requireNonNull((String) map.get("parent"));
        Map<String, Object> texturesMap = MiscUtils.castToMap(map.get("textures"), true);
        if (texturesMap != null) {
            this.texturesOverride = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : texturesMap.entrySet()) {
                if (entry.getValue() instanceof String p) {
                    this.texturesOverride.put(entry.getKey(), p);
                }
            }
        } else {
            this.texturesOverride = Collections.emptyMap();
        }
    }

    public Key path() {
        return path;
    }

    public String parentModelPath() {
        return parentModelPath;
    }

    public Map<String, String> texturesOverride() {
        return texturesOverride;
    }

    public JsonObject getJson() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parentModelPath);
        if (this.texturesOverride != null && !this.texturesOverride.isEmpty()) {
            JsonObject textures = new JsonObject();
            for (Map.Entry<String, String> entry : this.texturesOverride.entrySet()) {
                textures.addProperty(entry.getKey(), entry.getValue());
            }
            model.add("textures", textures);
        }
        return model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelGeneration that = (ModelGeneration) o;
        return this.path.equals(that.path) && parentModelPath.equals(that.parentModelPath) && Objects.equals(texturesOverride, that.texturesOverride);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + parentModelPath.hashCode();
        result = 31 * result + texturesOverride.hashCode();
        return result;
    }
}
