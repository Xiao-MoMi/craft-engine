package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// 只保存模型与局部变换烘焙后的几何；场景、显示条件和骨骼由各渲染出口决定。
public record BedrockItemGeometry(JavaModel model, JsonArray cubes) {
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
                JsonObject cube = original.deepCopy();
                cube.add("uv", faces);
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
