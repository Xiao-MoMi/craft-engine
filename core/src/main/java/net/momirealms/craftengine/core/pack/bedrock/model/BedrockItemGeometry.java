package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.Map;

// cubes 无法直接表达的仿射变换保留在 transformation 中，由实体骨骼或图标投影应用。
public record BedrockItemGeometry(JavaModel model, JsonArray cubes, Matrix4fc transformation) {
    public static JsonArray selectMaterialFaces(JsonArray source, String material) {
        // 一个立方体的不同面可能属于不同材质层，只复制当前层的面，原始几何留给后续层使用。
        JsonArray cubes = new JsonArray();
        for (JsonElement value : source) {
            JsonObject original = value.getAsJsonObject();
            JsonObject faces = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : original.getAsJsonObject("uv").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                if (face.get("material_instance").getAsString().equals(material)) {
                    JsonObject selected = face.deepCopy();
                    selected.remove("material_instance");
                    faces.add(entry.getKey(), selected);
                }
            }
            if (!faces.isEmpty()) {
                JsonObject cube = new JsonObject();
                // UV 已按材质筛选，不能再复制整套 UV 后立即丢弃。
                for (Map.Entry<String, JsonElement> entry : original.entrySet()) {
                    cube.add(entry.getKey(), entry.getKey().equals("uv") ? faces : entry.getValue().deepCopy());
                }
                cubes.add(cube);
            }
        }
        return cubes;
    }

    // color 是烘焙贴图的静态颜色；dyeColor 非空时保留动态染色，并记录未染色实例的默认色。
    public record Material(TextureData data, int color, @Nullable Integer dyeColor) {
        public Material(TextureData data, int color) {
            this(data, color, null);
        }
    }
}
