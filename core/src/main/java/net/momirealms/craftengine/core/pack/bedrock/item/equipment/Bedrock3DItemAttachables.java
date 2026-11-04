package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTextures;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

public final class Bedrock3DItemAttachables {
    private final Key id;
    private final Path root;
    private final String resource;
    private final String prefix;
    private final List<Stage> stages;
    private final List<Material> materials;
    private final JsonArray poseBones;
    private final JsonObject textureNames = new JsonObject();
    private final JsonObject geometryNames = new JsonObject();
    private final JsonObject animationNames = new JsonObject();
    private final JsonArray geometries = new JsonArray();
    private final JsonObject renderControllers = new JsonObject();
    private final JsonArray activeControllers = new JsonArray();
    private final JsonArray animate = new JsonArray();
    private List<BedrockItemAttachables.View> contextViews;
    private BedrockEquipmentPose contextPose;

    private Bedrock3DItemAttachables(Key id, Path root, List<Stage> stages, List<Material> materials, JsonArray poseBones) {
        this.id = id;
        this.root = root;
        this.resource = id.namespace() + '/' + id.value();
        this.prefix = ResourcePackUtils.bedrockResourceIdentifier(id);
        this.stages = stages;
        this.materials = materials;
        this.poseBones = poseBones;
    }

    public static void write(Key id,
                      List<Stage> stages,
                      List<Material> materials,
                      JsonArray preAnimation,
                      JsonArray poseBones,
                      JsonObject poseAnimations,
                      JsonArray poseAnimate,
                      Path root) throws IOException {
        Bedrock3DItemAttachables writer = new Bedrock3DItemAttachables(id, root, stages, materials, poseBones);
        writer.writeMaterialLayers();
        writer.writeAnimations(poseAnimations, poseAnimate);
        writer.writeAttachable(preAnimation);
    }

    // 逐层写出贴图，再将各层的几何和渲染控制器分别汇总到该物品的 .geo.json 和 .render_controllers.json 文件中
    private void writeMaterialLayers() throws IOException {
        this.textureNames.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        for (int layer = 0; layer < this.materials.size(); layer++) {
            this.writeMaterialLayer(layer, this.materials.get(layer));
        }
        if (this.contextViews != null && this.activeControllers.isEmpty()) {
            // An item can have a GUI icon and an explicit empty model in every equipment slot.
            String mesh = "geometry." + this.prefix + ".empty";
            JsonObject geometry = new JsonObject();
            geometry.add("description", GsonHelper.createObject("identifier", new JsonPrimitive(mesh)));
            JsonArray bones = new JsonArray();
            bones.add(GsonHelper.createObject("name", new JsonPrimitive("root")));
            geometry.add("bones", bones);
            this.geometries.add(geometry);
            this.geometryNames.addProperty("empty", mesh);
            JsonObject controller = new JsonObject();
            controller.addProperty("geometry", "Geometry.empty");
            String name = "controller.render." + this.prefix + ".empty";
            this.renderControllers.add(name, controller);
            this.activeControllers.add(name);
        }
        writeFile("1.21.0", "minecraft:geometry", this.geometries, this.root.resolve("models/entity/" + this.resource + ".geo.json"));
        writeFile("1.10.0", "render_controllers", this.renderControllers, this.root.resolve("render_controllers/" + this.resource + ".render_controllers.json"));
    }

    static void writeDisplayContexts(Key id, Key headId, List<Stage> stages, List<BedrockItemAttachables.View> views, List<Material> materials, BedrockEquipmentPose pose, Path root) throws IOException {
        Bedrock3DItemAttachables writer = new Bedrock3DItemAttachables(id, root, stages, materials, new JsonArray());
        writer.contextViews = views;
        writer.contextPose = pose;
        writer.writeMaterialLayers();
        JsonObject animations = writer.contextAnimations(pose);
        writer.writeAnimations(animations, animations.isEmpty() ? new JsonArray() : pose.animate());
        JsonObject attachable = writer.writeAttachable(pose.scripts());
        if (headId != null) {
            // 头槽和手持使用不同物品 ID，但引用同一套几何、动画和贴图。
            attachable.getAsJsonObject("description").addProperty("identifier", headId.asString());
            writeFile("1.10.0", "minecraft:attachable", attachable, root.resolve("attachables/" + headId.namespace() + '/' + headId.value() + ".attachable.json"));
        }
    }

    private void writeMaterialLayer(int layer, Material material) throws IOException {
        String layerName = "layer" + layer;
        String texturePath = "textures/" + this.resource + "/hand/layer_" + layer;
        JsonArray frames = BedrockItemTextures.writeFrames(material.data(), material.color(), layerName, texturePath, this.textureNames, this.root);
        JsonArray defaultFrames = material.dyeColor() == null ? null : BedrockItemTextures.writeFrames(
                material.data(),
                material.dyeColor(),
                layerName + "_default",
                texturePath + "_default",
                this.textureNames,
                this.root
        );

        // 各阶段共用这一层的贴图。每个有可见面的阶段各自注册控制器，由 ce_stage 选中当前阶段。
        Map<JsonArray, JsonObject> layerGeometries = new HashMap<>();
        for (int index = 0; index < this.stages.size(); index++) {
            Stage stage = this.stages.get(index);
            JsonArray cubes = selectMaterialFaces(stage.cubes(), layerName);
            if (cubes.isEmpty()) {
                continue;
            }
            String alias = "stage_" + index + '_' + layerName;
            if (this.contextViews != null) {
                // Composite parts can have different display transforms and be visible simultaneously.
                String geometryId = "geometry." + this.prefix + '.' + alias;
                String prefix = "p" + index + "_";
                JsonObject geometry = BedrockItemAttachables.displayGeometry(stage.model(), cubes, geometryId, modelRadius(cubes), prefix);
                if (this.contextPose.views().contains(this.contextViews.get(index)) && !this.contextPose.bones().isEmpty()) {
                    // 每个部件有独立的挂点；蓄力变换作用于该部件的 Java display 变换之前。
                    insertPoseBones(geometry, prefix, this.contextPose.bones());
                }
                this.geometries.add(geometry);
                this.geometryNames.addProperty(alias, geometryId);
            } else {
                this.registerGeometry(alias, cubes, stage.model(), layerGeometries);
            }
            String controllerId = "controller.render." + this.prefix + '.' + alias;
            this.renderControllers.add(controllerId, renderController(alias, material.data().animation(), frames, defaultFrames));
            JsonObject active = new JsonObject();
            active.addProperty(controllerId, this.contextViews != null ? stage.condition() : "(context.item_slot == 'main_hand' || context.item_slot == 'off_hand') && variable.ce_stage == " + index);
            this.activeControllers.add(active);
        }
    }

    private void registerGeometry(String alias, JsonArray cubes, JavaModel model, Map<JsonArray, JsonObject> layerGeometries) {
        float radius = modelRadius(cubes);
        JsonObject geometry = layerGeometries.get(cubes);
        if (geometry == null) {
            geometry = createGeometry("geometry." + this.prefix + '.' + alias, cubes, model, radius, this.poseBones);
            layerGeometries.put(cubes, geometry);
            this.geometries.add(geometry);
        } else {
            // 同一组面可用于不同的手持姿态，裁剪范围要覆盖每个阶段的 display。
            BedrockItemAttachables.expandDisplayBounds(geometry.getAsJsonObject("description"), model, radius);
        }
        // 控制器按阶段别名引用几何；不同阶段的别名可以指向同一个几何 ID。
        this.geometryNames.addProperty(alias, geometry.getAsJsonObject("description").get("identifier").getAsString());
    }

    private static JsonObject renderController(String alias, TextureAnimation animation, JsonArray frames, JsonArray defaultFrames) {
        JsonObject controller = new JsonObject();
        controller.addProperty("geometry", "Geometry." + alias);

        JsonObject textureArrays = GsonHelper.createObject("array.frames", frames);
        if (defaultFrames != null) {
            textureArrays.add("array.defaults", defaultFrames);
        }
        controller.add("arrays", GsonHelper.createObject("textures", textureArrays));

        // 将运行秒数换算为 tick，按动画总时长循环，再用采样间隔换算成贴图帧索引。
        String frame = animation == null ? "0" : "math.floor(math.mod(query.life_time * 20, " + animation.duration() + ") / " + animation.sampleInterval() + ")";
        String texture = "array.frames[" + frame + "]";
        JsonArray textures = new JsonArray();
        // 未染色时直接使用烘焙了默认色的贴图；只有实际染色的实例需要动态材质。
        textures.add(defaultFrames == null ? texture : "variable.ce_hand_dye < 0 ? array.defaults[" + frame + "] : " + texture);
        textures.add("Texture.enchanted");
        controller.add("textures", textures);

        JsonObject material = new JsonObject();
        if (defaultFrames == null) {
            material.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        } else {
            material.addProperty("*", "variable.ce_hand_dye < 0 ? (variable.is_enchanted ? Material.enchanted : Material.default)" + " : (variable.is_enchanted ? Material.dyed_enchanted : Material.dyed)");
            controller.add("color", handColor());
        }
        JsonArray materials = new JsonArray();
        materials.add(material);
        controller.add("materials", materials);
        return controller;
    }

    private JsonObject contextAnimations(BedrockEquipmentPose pose) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> action : pose.animations().entrySet()) {
            JsonObject animation = action.getValue().getAsJsonObject();
            JsonObject bones = new JsonObject();
            for (int i = 0; i < this.contextViews.size(); i++) {
                if (pose.views().contains(this.contextViews.get(i))) {
                    for (Map.Entry<String, JsonElement> bone : animation.getAsJsonObject("bones").entrySet()) {
                        bones.add("p" + i + "_" + bone.getKey(), bone.getValue());
                    }
                }
            }
            if (!bones.isEmpty()) {
                JsonObject bound = animation.deepCopy();
                bound.add("bones", bones);
                result.add(action.getKey(), bound);
            }
        }
        return result;
    }

    private void writeAnimations(JsonObject poseAnimations, JsonArray poseAnimate) throws IOException {
        JsonObject animations = new JsonObject();
        String animationPrefix = "animation." + this.prefix + '.';
        // 先登记各阶段的 Java display 变换，再把拉弓、格挡等物品专属动作追加到播放列表。
        for (int index = 0; index < this.stages.size(); index++) {
            Stage stage = this.stages.get(index);
            if (this.contextViews != null) {
                BedrockItemAttachables.contextAnimation(this.contextViews.get(index), stage.model(), animationPrefix, "p" + index + "_", stage.condition(), animations, this.animationNames, this.animate);
            } else {
                BedrockItemAttachables.displayAnimations(stage.model(), animationPrefix, index, animations, this.animationNames, this.animate);
            }
        }
        for (Map.Entry<String, JsonElement> pose : poseAnimations.entrySet()) {
            String name = animationPrefix + pose.getKey();
            animations.add(name, pose.getValue());
            this.animationNames.addProperty(pose.getKey(), name);
        }
        this.animate.addAll(poseAnimate);
        writeFile("1.10.0", "animations", animations, this.root.resolve("animations/" + this.resource + ".animation.json"));
    }

    private JsonObject writeAttachable(JsonArray preAnimation) throws IOException {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", this.id.asString());
        JsonObject renderMaterials = new JsonObject();
        renderMaterials.addProperty("default", "entity_alphatest");
        renderMaterials.addProperty("enchanted", "entity_alphatest_glint");
        for (Material material : this.materials) {
            if (material.dyeColor() != null) {
                renderMaterials.addProperty("dyed", "entity_alphatest_change_color");
                renderMaterials.addProperty("dyed_enchanted", "entity_alphatest_change_color_glint");
                break;
            }
        }
        description.add("materials", renderMaterials);
        description.add("textures", this.textureNames);
        description.add("geometry", this.geometryNames);
        description.add("animations", this.animationNames);
        // pre_animation 先计算 ce_stage 等变量，动画和渲染控制器再用这些值选择当前姿态与几何。
        JsonObject scripts = GsonHelper.createObject("animate", this.animate);
        scripts.add("pre_animation", preAnimation);
        description.add("scripts", scripts);
        description.add("render_controllers", this.activeControllers);
        JsonObject attachable = GsonHelper.createObject("description", description);
        writeFile("1.10.0", "minecraft:attachable", attachable, this.root.resolve("attachables/" + this.resource + ".attachable.json"));
        return attachable;
    }

    private static JsonObject handColor() {
        JsonObject color = new JsonObject();
        // ce_hand_dye 保存 0xRRGGBB 整数，材质需要拆成三个 0～1 的颜色通道。
        String rgb = "(variable.ce_hand_dye < 0 ? 16777215 : variable.ce_hand_dye)";
        color.addProperty("r", "math.floor(" + rgb + " / 65536) / 255");
        color.addProperty("g", "math.mod(math.floor(" + rgb + " / 256), 256) / 255");
        color.addProperty("b", "math.mod(" + rgb + ", 256) / 255");
        color.addProperty("a", 1);
        return color;
    }

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

    private static float modelRadius(JsonArray cubes) {
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
        return radius;
    }

    private static JsonObject createGeometry(String identifier, JsonArray cubes, JavaModel model, float radius, JsonArray poseBones) {
        JsonObject geometry = BedrockItemAttachables.displayGeometry(model, cubes, identifier, radius);
        if (!poseBones.isEmpty()) {
            insertPoseBones(geometry, "", poseBones);
        }
        return geometry;
    }

    private static void insertPoseBones(JsonObject geometry, String prefix, JsonArray poseBones) {
        String slot = prefix + "ce_slot";
        JsonArray bound = poseBones.deepCopy();
        if (!prefix.isEmpty()) {
            for (JsonElement value : bound) {
                JsonObject bone = value.getAsJsonObject();
                bone.addProperty("name", prefix + bone.get("name").getAsString());
                bone.addProperty("parent", prefix + bone.get("parent").getAsString());
            }
        }
        // 把姿态骨骼插在手持挂点 ce_slot 与原有子骨骼之间，使整个模型继承物品的姿态变换。
        JsonArray bones = new JsonArray();
        for (JsonElement value : geometry.getAsJsonArray("bones")) {
            JsonObject bone = value.getAsJsonObject();
            if (bone.has("parent") && bone.get("parent").getAsString().equals(slot)) {
                bone.addProperty("parent", bound.get(bound.size() - 1).getAsJsonObject().get("name").getAsString());
            }
            bones.add(bone);
            if (bone.get("name").getAsString().equals(slot)) {
                bones.addAll(bound);
            }
        }
        geometry.add("bones", bones);
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

    public record Material(TextureData data, int color, Integer dyeColor) {
        public Material(TextureData data, int color) {
            this(data, color, null);
        }
    }
}
