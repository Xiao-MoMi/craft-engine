package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.HashMap;
import java.util.Map;

// cubes 无法直接表达的仿射变换保留在 transformation 中，由实体骨骼或图标投影应用。
public record BedrockItemGeometry(JavaModel model, JsonArray cubes, Matrix4fc transformation) {
    public Map<String, JsonArray> splitMaterialFaces() {
        Map<String, JsonArray> layers = new HashMap<>();
        Map<String, JsonObject> facesByLayer = new HashMap<>();
        for (JsonElement value : this.cubes) {
            JsonObject original = value.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : original.getAsJsonObject("uv").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                String material = face.get("material_instance").getAsString();
                JsonObject selected = new JsonObject();
                for (Map.Entry<String, JsonElement> property : face.entrySet()) {
                    if (!property.getKey().equals("material_instance")) {
                        selected.add(property.getKey(), property.getValue());
                    }
                }
                facesByLayer.computeIfAbsent(material, ignored -> new JsonObject()).add(entry.getKey(), selected);
            }
            for (Map.Entry<String, JsonObject> layer : facesByLayer.entrySet()) {
                JsonObject copy = new JsonObject();
                // 展示实体会平移 origin/pivot。各层的立方体独占这些坐标，UV 子节点只读。
                for (Map.Entry<String, JsonElement> property : original.entrySet()) {
                    copy.add(property.getKey(), property.getKey().equals("uv") ? layer.getValue() : property.getValue().deepCopy());
                }
                layers.computeIfAbsent(layer.getKey(), ignored -> new JsonArray()).add(copy);
            }
            facesByLayer.clear();
        }
        return layers;
    }

    // color 是烘焙贴图的静态颜色；dyeColor 非空时保留动态染色，并记录未染色实例的默认色。
    public record Material(TextureData data, int color, @Nullable Integer dyeColor) {
        public Material(TextureData data, int color) {
            this(data, color, null);
        }
    }
}
