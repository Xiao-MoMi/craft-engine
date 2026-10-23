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

public final class Bedrock3DBowAttachables {
    private static final String USE_X = "ce_bow_use_x";
    private static final String USE_Y = "ce_bow_use_y";
    private static final String USE_Z = "ce_bow_use_z";
    private static final String CHARGE = "ce_bow_charge";
    private static final String AIM = "ce_bow_aim";

    private Bedrock3DBowAttachables() {
    }

    public static void write(Key id, List<Stage> stages, List<Material> materials, int maximumUseTicks, Path root) throws IOException {
        String resource = id.namespace() + '/' + id.value();
        String prefix = id.namespace() + '.' + id.value();
        JsonObject textureNames = new JsonObject();
        textureNames.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        JsonObject geometryNames = new JsonObject();
        JsonArray geometries = new JsonArray();
        JsonObject controllers = new JsonObject();
        JsonArray activeControllers = new JsonArray();
        for (int layer = 0; layer < materials.size(); layer++) {
            Material material = materials.get(layer);
            String name = "layer" + layer;
            String path = "textures/craftengine_bows/" + resource + '_' + name;
            JsonArray frames = writeTextures(material, name, path, textureNames, root);
            for (int stage = 0; stage < stages.size(); stage++) {
                JsonArray cubes = select(stages.get(stage).cubes(), name);
                if (cubes.isEmpty()) {
                    continue;
                }
                String alias = "stage_" + stage + '_' + name;
                String geometry = "geometry." + prefix + '.' + alias;
                geometryNames.addProperty(alias, geometry);
                geometries.add(geometry(geometry, cubes, stages.get(stage).model()));
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
        String useAnimation = "animation." + prefix + ".java_bow_use";
        animations.add(useAnimation, useAnimation());
        names.addProperty("java_bow_use", useAnimation);
        JsonObject using = new JsonObject();
        using.addProperty("java_bow_use", "context.is_first_person && variable.ce_using_item");
        animate.add(using);
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
        JsonArray preAnimation = BedrockItemAttachables.bowScripts(stages.stream().map(Stage::condition).toList(), maximumUseTicks);
        // Java 26.3 FirstPersonHandsAndItemsRenderer：使用时长包含帧插值，弓在 20 tick 达到满蓄力。
        preAnimation.add("variable.ce_bow_time = variable.ce_using_item ? variable.ce_use_ticks + query.frame_alpha - 1 : 0;");
        preAnimation.add("variable.ce_bow_power = variable.ce_bow_time / 20;");
        preAnimation.add("variable.ce_bow_power = math.min(1, (variable.ce_bow_power * variable.ce_bow_power + variable.ce_bow_power * 2) / 3);");
        preAnimation.add("variable.ce_bow_side = context.item_slot == 'off_hand' ? -1 : 1;");
        // Java sin 使用弧度，Molang 使用角度；平移由方块单位转为模型单位。
        preAnimation.add("variable.ce_bow_shake = variable.ce_bow_power > 0.1 ? math.sin((variable.ce_bow_time - 0.1) * 1.3 * 180 / math.pi) * (variable.ce_bow_power - 0.1) * 0.004 * 16 : 0;");
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
                String suffix = "_f" + tick / interval;
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

    private static JsonObject geometry(String identifier, JsonArray cubes, JavaModel model) {
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
            if (bone.has("parent") && bone.get("parent").getAsString().equals("ce_slot")) {
                bone.addProperty("parent", AIM);
            }
            bones.add(bone);
            if (bone.get("name").getAsString().equals("ce_slot")) {
                // Java 使用姿态先于模型 display；分开旋转与非均匀缩放，保留矩阵乘法顺序。
                bones.add(useBone(USE_X, "ce_slot"));
                bones.add(useBone(USE_Y, USE_X));
                bones.add(useBone(USE_Z, USE_Y));
                bones.add(useBone(CHARGE, USE_Z));
                bones.add(useBone(AIM, CHARGE));
            }
        }
        geometry.add("bones", bones);
        return geometry;
    }

    private static JsonObject useBone(String name, String parent) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        bone.addProperty("parent", parent);
        bone.add("pivot", VectorUtils.toJson(0, 8, 0));
        return bone;
    }

    private static JsonObject useAnimation() {
        JsonObject x = new JsonObject();
        JsonArray position = new JsonArray();
        position.add("variable.ce_bow_side * 0.2785682 * 16");
        position.add(0.18344387f * 16);
        position.add(0.15731531f * 16);
        x.add("position", position);
        x.add("rotation", VectorUtils.toJson(13.935f, 0, 0));
        JsonObject y = new JsonObject();
        JsonArray rotationY = new JsonArray();
        rotationY.add(0);
        rotationY.add("variable.ce_bow_side * -35.3");
        rotationY.add(0);
        y.add("rotation", rotationY);
        JsonObject z = new JsonObject();
        JsonArray rotationZ = new JsonArray();
        rotationZ.add(0);
        rotationZ.add(0);
        rotationZ.add("variable.ce_bow_side * -9.785");
        z.add("rotation", rotationZ);
        JsonObject charge = new JsonObject();
        JsonArray movement = new JsonArray();
        movement.add(0);
        movement.add("variable.ce_bow_shake");
        movement.add("variable.ce_bow_power * 0.04 * 16");
        charge.add("position", movement);
        JsonArray scale = new JsonArray();
        scale.add(1);
        scale.add(1);
        scale.add("1 + variable.ce_bow_power * 0.2");
        charge.add("scale", scale);
        JsonObject aim = new JsonObject();
        JsonArray rotationAim = new JsonArray();
        rotationAim.add(0);
        rotationAim.add("variable.ce_bow_side * 45");
        rotationAim.add(0);
        aim.add("rotation", rotationAim);
        JsonObject bones = new JsonObject();
        bones.add(USE_X, x);
        bones.add(USE_Y, y);
        bones.add(USE_Z, z);
        bones.add(CHARGE, charge);
        bones.add(AIM, aim);
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
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
