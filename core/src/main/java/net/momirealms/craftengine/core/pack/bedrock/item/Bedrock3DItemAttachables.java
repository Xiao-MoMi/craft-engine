package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class Bedrock3DItemAttachables {
    private Bedrock3DItemAttachables() {
    }

    static void write(Key id, List<Stage> stages, List<Material> materials, JsonArray preAnimation,
                      JsonArray poseBones, JsonObject poseAnimations, JsonArray poseAnimate, Path root) throws IOException {
        String resource = id.namespace() + '/' + id.value();
        String prefix = ResourcePackUtils.bedrockResourceIdentifier(id);
        JsonObject textureNames = new JsonObject();
        textureNames.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        JsonObject geometryNames = new JsonObject();
        JsonArray geometries = new JsonArray();
        JsonObject controllers = new JsonObject();
        JsonArray activeControllers = new JsonArray();
        for (int layer = 0; layer < materials.size(); layer++) {
            Material material = materials.get(layer);
            String name = "layer" + layer;
            String path = "textures/" + resource + "/hand/layer_" + layer;
            JsonArray frames = writeTextures(material, name, path, textureNames, root);
            for (int stage = 0; stage < stages.size(); stage++) {
                JsonArray cubes = select(stages.get(stage).cubes(), name);
                if (cubes.isEmpty()) {
                    continue;
                }
                String alias = "stage_" + stage + '_' + name;
                String geometry = "geometry." + prefix + '.' + alias;
                geometryNames.addProperty(alias, geometry);
                geometries.add(geometry(geometry, cubes, stages.get(stage).model(), poseBones));
                JsonObject controller = new JsonObject();
                controller.addProperty("geometry", "Geometry." + alias);
                JsonObject textureArrays = new JsonObject();
                textureArrays.add("array.frames", frames);
                JsonObject arrays = new JsonObject();
                arrays.add("textures", textureArrays);
                controller.add("arrays", arrays);
                JsonArray textures = new JsonArray();
                TextureAnimation animation = material.data().animation();
                if (animation == null) {
                    textures.add("array.frames[0]");
                } else {
                    // 各材质按自身时长播放，切换拉弓阶段时不重置贴图动画。
                    textures.add("array.frames[math.floor(math.mod(query.life_time * 20, "
                            + animation.duration() + ") / " + animation.sampleInterval() + ")]");
                }
                textures.add("Texture.enchanted");
                controller.add("textures", textures);
                JsonArray renderMaterials = new JsonArray();
                JsonObject renderMaterial = new JsonObject();
                renderMaterial.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
                renderMaterials.add(renderMaterial);
                controller.add("materials", renderMaterials);
                String controllerId = "controller.render." + prefix + '.' + alias;
                controllers.add(controllerId, controller);
                JsonObject active = new JsonObject();
                active.addProperty(controllerId, "(context.item_slot == 'main_hand' || context.item_slot == 'off_hand') && variable.ce_stage == " + stage);
                activeControllers.add(active);
            }
        }
        writeFile("1.21.0", "minecraft:geometry", geometries, root.resolve("models/entity/" + resource + ".geo.json"));
        writeFile("1.10.0", "render_controllers", controllers, root.resolve("render_controllers/" + resource + ".render_controllers.json"));

        JsonObject animations = new JsonObject();
        JsonObject names = new JsonObject();
        JsonArray animate = new JsonArray();
        for (int stage = 0; stage < stages.size(); stage++) {
            BedrockItemAttachables.displayAnimations(stages.get(stage).model(), "animation." + prefix + '.', stage, animations, names, animate);
        }
        for (Map.Entry<String, JsonElement> pose : poseAnimations.entrySet()) {
            String name = "animation." + prefix + '.' + pose.getKey();
            animations.add(name, pose.getValue());
            names.addProperty(pose.getKey(), name);
        }
        animate.addAll(poseAnimate);
        writeFile("1.10.0", "animations", animations, root.resolve("animations/" + resource + ".animation.json"));
        JsonObject description = new JsonObject();
        description.addProperty("identifier", id.asString());
        JsonObject renderMaterials = new JsonObject();
        renderMaterials.addProperty("default", "entity_alphatest");
        renderMaterials.addProperty("enchanted", "entity_alphatest_glint");
        description.add("materials", renderMaterials);
        description.add("textures", textureNames);
        description.add("geometry", geometryNames);
        description.add("animations", names);
        JsonObject scripts = new JsonObject();
        scripts.add("animate", animate);
        scripts.add("pre_animation", preAnimation);
        description.add("scripts", scripts);
        description.add("render_controllers", activeControllers);
        JsonObject attachable = new JsonObject();
        attachable.add("description", description);
        writeFile("1.10.0", "minecraft:attachable", attachable, root.resolve("attachables/" + resource + ".attachable.json"));
    }

    private static JsonArray writeTextures(Material material, String name, String path, JsonObject textures, Path root) throws IOException {
        TextureData data = material.data();
        TextureAnimation animation = data.animation();
        JsonArray frames = new JsonArray();
        if (animation == null) {
            BufferedImage image = material.color() == WHITE ? data.image() : ImageUtils.multiplyColor(data.image(), material.color());
            ImageUtils.writePng(image, root.resolve(path + ".png"));
            textures.addProperty(name, path);
            frames.add("Texture." + name);
        } else {
            int interval = animation.sampleInterval();
            BufferedImage image = new BufferedImage(animation.width(), animation.height(), BufferedImage.TYPE_INT_ARGB);
            for (int tick = 0; tick < animation.duration(); tick += interval) {
                animation.render(tick, material.color(), image);
                String suffix = "_frame_" + tick / interval;
                ImageUtils.writePng(image, root.resolve(path + suffix + ".png"));
                textures.addProperty(name + suffix, path + suffix);
                frames.add("Texture." + name + suffix);
            }
        }
        return frames;
    }

    private static JsonArray select(JsonArray source, String material) {
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

    private static JsonObject geometry(String identifier, JsonArray cubes, JavaModel model, JsonArray poseBones) {
        float radius = 0;
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            Vector3f halfSize = VectorUtils.vector3f(cube.getAsJsonArray("size")).mul(0.5f);
            Vector3f center = VectorUtils.vector3f(cube.getAsJsonArray("origin")).add(halfSize);
            float distance = center.distance(0, 8, 0);
            if (cube.has("rotation")) {
                Vector3f pivot = VectorUtils.vector3f(cube.getAsJsonArray("pivot"));
                // 原始元素可绕任意支点旋转，包围球还需覆盖中心绕支点的轨迹。
                distance = center.distance(pivot) + pivot.distance(0, 8, 0);
            }
            radius = Math.max(radius, distance + halfSize.length());
        }
        JsonObject geometry = BedrockItemAttachables.displayGeometry(model, cubes, identifier, radius);
        JsonArray bones = new JsonArray();
        for (JsonElement value : geometry.getAsJsonArray("bones")) {
            JsonObject bone = value.getAsJsonObject();
            if (!poseBones.isEmpty() && bone.has("parent") && bone.get("parent").getAsString().equals("ce_slot")) {
                bone.addProperty("parent", poseBones.get(poseBones.size() - 1).getAsJsonObject().get("name").getAsString());
            }
            bones.add(bone);
            if (bone.get("name").getAsString().equals("ce_slot")) {
                bones.addAll(poseBones.deepCopy());
            }
        }
        geometry.add("bones", bones);
        return geometry;
    }

    private static void writeFile(String version, String key, JsonElement value, Path path) throws IOException {
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, version);
        file.add(key, value);
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(file, path);
    }

    public record Stage(JavaModel model, JsonArray cubes, String condition) {
    }

    public record Material(TextureData data, int color) {
    }
}
