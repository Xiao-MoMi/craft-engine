package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTextures;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry.Material;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.ANIMATION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.ATTACHABLE;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.GEOMETRY;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.RENDER_CONTROLLER;

public final class BedrockItem3DAttachables {
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
    private final JsonObject localChannels = new JsonObject();
    private int[] contextGeometryIndices;
    private BedrockAttachablePose contextPose;
    private BedrockEquipmentResources.Appearance armor;

    private BedrockItem3DAttachables(Key id, Path root, List<Stage> stages, List<Material> materials, JsonArray poseBones) {
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
        BedrockItem3DAttachables writer = new BedrockItem3DAttachables(id, root, stages, materials, poseBones);
        writer.writeMaterialLayers();
        writer.writeAnimations(poseAnimations, poseAnimate);
        writer.writeAttachable(preAnimation);
    }

    static void writeDisplayContexts(Key id, Key headId, List<Stage> stages, List<Material> materials, BedrockAttachablePose pose, BedrockEquipmentResources.Appearance armor, Path root) throws IOException {
        BedrockItem3DAttachables writer = new BedrockItem3DAttachables(id, root, stages, materials, new JsonArray());
        writer.contextPose = pose;
        writer.armor = armor;
        writer.groupContextGeometries();
        writer.writeMaterialLayers();
        JsonObject animations = writer.contextAnimations(pose);
        writer.writeAnimations(animations, animations.isEmpty() ? new JsonArray() : pose.animate());
        JsonObject attachable = writer.writeAttachable(pose.scripts());
        if (headId != null) {
            // 头槽和手持使用不同物品 ID，但引用同一套几何、动画和贴图。
            attachable.getAsJsonObject("description").addProperty("identifier", headId.asString());
            ResourcePackUtils.writeBedrockJson(ATTACHABLE, "minecraft:attachable", attachable, root.resolve("attachables/" + headId.namespace() + '/' + headId.value() + ".attachable.json"));
        }
    }

    // 逐层写出贴图，再将各层的几何和渲染控制器分别汇总到该物品的 .geo.json 和 .render_controllers.json 文件中
    private void writeMaterialLayers() throws IOException {
        this.textureNames.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        for (int layer = 0; layer < this.materials.size(); layer++) {
            this.writeMaterialLayer(layer, this.materials.get(layer));
        }
        if (this.contextPose != null && this.activeControllers.isEmpty()) {
            // 物品可以有背包图标，但所有装备位置都显式为空；仍需生成空几何供 attachable 引用。
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
        if (this.armor != null) {
            this.armor.addResources(this.prefix, this.textureNames, this.geometryNames, this.renderControllers, this.activeControllers);
        }
        ResourcePackUtils.writeBedrockJson(GEOMETRY, "minecraft:geometry", this.geometries, this.root.resolve("models/entity/" + this.resource + ".geo.json"));
        ResourcePackUtils.writeBedrockJson(RENDER_CONTROLLER, "render_controllers", this.renderControllers, this.root.resolve("render_controllers/" + this.resource + ".render_controllers.json"));
    }

    private void groupContextGeometries() {
        // 每个阶段映射到负责写出几何的首个阶段；控制器仍按原阶段生成，保留各自的显示条件。
        this.contextGeometryIndices = new int[this.stages.size()];
        Map<ContextGeometry, List<ContextGroup>> groups = new HashMap<>();
        for (int index = 0; index < this.stages.size(); index++) {
            Stage stage = this.stages.get(index);
            BedrockItemAttachables.View view = stage.view();
            // posed 也参与分组：插入了使用动作骨骼的几何不能与未插入的版本共用。
            ContextGeometry key = new ContextGeometry(stage.geometry(), this.contextPose.views().contains(view));
            List<ContextGroup> candidates = groups.computeIfAbsent(key, ignored -> new ArrayList<>());
            ContextGroup selected = null;
            for (ContextGroup candidate : candidates) {
                if (!candidate.views().contains(view)) {
                    selected = candidate;
                    break;
                }
            }
            // 不同场景互斥，可共用骨骼；同一场景的部件可能同时出现，必须保持独立。
            if (selected == null) {
                selected = new ContextGroup(index, EnumSet.noneOf(BedrockItemAttachables.View.class));
                candidates.add(selected);
            }
            selected.views().add(view);
            this.contextGeometryIndices[index] = selected.index();
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
        Map<LocalGeometry, JsonObject> layerGeometries = this.contextPose == null ? new HashMap<>() : null;
        JsonArray[] contextCubes = this.contextPose == null ? null : new JsonArray[this.stages.size()];
        for (int index = 0; index < this.stages.size(); index++) {
            Stage stage = this.stages.get(index);
            String alias = "stage_" + index + '_' + layerName;
            if (contextCubes != null) {
                if (!this.registerContextGeometry(index, layerName, alias, contextCubes)) {
                    continue;
                }
            } else {
                JsonArray cubes = BedrockItemGeometry.selectMaterialFaces(stage.geometry().cubes(), layerName);
                if (cubes.isEmpty()) {
                    continue;
                }
                this.registerGeometry(alias, cubes, stage.geometry(), layerGeometries);
            }
            String controllerId = "controller.render." + this.prefix + '.' + alias;
            this.renderControllers.add(controllerId, renderController(alias, material.data().animation(), frames, defaultFrames));
            JsonObject active = new JsonObject();
            active.addProperty(controllerId, this.contextPose != null ? stage.condition() : "(context.item_slot == 'main_hand' || context.item_slot == 'off_hand') && variable.ce_stage == " + index);
            this.activeControllers.add(active);
        }
    }

    private boolean registerContextGeometry(int index, String layerName, String alias, JsonArray[] contextCubes) {
        Stage stage = this.stages.get(index);
        int geometryIndex = this.contextGeometryIndices[index];
        JsonArray cubes = contextCubes[geometryIndex];
        if (cubes == null) {
            cubes = BedrockItemGeometry.selectMaterialFaces(stage.geometry().cubes(), layerName);
            contextCubes[geometryIndex] = cubes;
        }
        if (cubes.isEmpty()) {
            return false;
        }
        String geometryId = "geometry." + this.prefix + ".stage_" + geometryIndex + '_' + layerName;
        // 缓存仅属于当前材质层；首阶段写出几何，后续场景只登记别名，不修改共享 cubes。
        if (geometryIndex == index) {
            String prefix = "p" + geometryIndex + "_";
            JsonObject geometry = BedrockItemAttachables.displayGeometry(stage.geometry().model(), cubes, geometryId,
                    modelRadius(cubes, stage.geometry().transformation()), prefix);
            if (this.contextPose.views().contains(stage.view()) && !this.contextPose.bones().isEmpty()) {
                // 每个部件有独立的挂点；蓄力变换作用于该部件的 Java display 变换之前。
                insertPoseBones(geometry, prefix, this.contextPose.bones());
            }
            this.addLocalTransform(geometry, stage.geometry().transformation(), prefix);
            this.geometries.add(geometry);
        }
        this.geometryNames.addProperty(alias, geometryId);
        return true;
    }

    private void registerGeometry(String alias, JsonArray cubes, BedrockItemGeometry source, Map<LocalGeometry, JsonObject> layerGeometries) {
        JavaModel model = source.model();
        float radius = modelRadius(cubes, source.transformation());
        LocalGeometry key = new LocalGeometry(cubes, source.transformation());
        JsonObject geometry = layerGeometries.get(key);
        if (geometry == null) {
            geometry = createGeometry("geometry." + this.prefix + '.' + alias, cubes, model, radius, this.poseBones);
            this.addLocalTransform(geometry, source.transformation(), alias + "_");
            layerGeometries.put(key, geometry);
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

    private JsonObject contextAnimations(BedrockAttachablePose pose) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> action : pose.animations().entrySet()) {
            JsonObject animation = action.getValue().getAsJsonObject();
            JsonObject bones = new JsonObject();
            for (int i = 0; i < this.stages.size(); i++) {
                if (pose.views().contains(this.stages.get(i).view())) {
                    for (Map.Entry<String, JsonElement> bone : animation.getAsJsonObject("bones").entrySet()) {
                        bones.add("p" + this.contextGeometryIndices[i] + "_" + bone.getKey(), bone.getValue());
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
            if (this.contextPose != null) {
                BedrockItemAttachables.contextAnimation(stage.view(), stage.geometry().model(), animationPrefix, "p" + this.contextGeometryIndices[index] + "_", stage.condition(), animations, this.animationNames, this.animate);
            } else {
                BedrockItemAttachables.displayAnimations(stage.geometry().model(), animationPrefix, index, animations, this.animationNames, this.animate);
            }
        }
        for (Map.Entry<String, JsonElement> pose : poseAnimations.entrySet()) {
            String name = animationPrefix + pose.getKey();
            animations.add(name, pose.getValue());
            this.animationNames.addProperty(pose.getKey(), name);
        }
        if (!this.localChannels.isEmpty()) {
            JsonObject local = new JsonObject();
            local.addProperty("loop", true);
            local.add("bones", this.localChannels);
            animations.add(animationPrefix + "local", local);
            this.animationNames.addProperty("local", animationPrefix + "local");
            this.animate.add("local");
        }
        this.animate.addAll(poseAnimate);
        ResourcePackUtils.writeBedrockJson(ANIMATION, "animations", animations, this.root.resolve("animations/" + this.resource + ".animation.json"));
    }

    private JsonObject writeAttachable(JsonArray preAnimation) throws IOException {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", this.id.asString());
        JsonObject renderMaterials = new JsonObject();
        renderMaterials.addProperty("default", "entity_alphatest_one_sided");
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
        if (this.armor != null) {
            this.armor.addMaterialsAndScripts(renderMaterials, scripts, this.animationNames);
        }
        description.add("scripts", scripts);
        description.add("render_controllers", this.activeControllers);
        JsonObject attachable = GsonHelper.createObject("description", description);
        ResourcePackUtils.writeBedrockJson(ATTACHABLE, "minecraft:attachable", attachable, this.root.resolve("attachables/" + this.resource + ".attachable.json"));
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

    private void addLocalTransform(JsonObject geometry, Matrix4fc transformation, String prefix) {
        if (BedrockModelTransform.isIdentity(transformation)) {
            return;
        }
        JsonArray bones = geometry.getAsJsonArray("bones");
        for (JsonElement value : bones) {
            JsonObject bone = value.getAsJsonObject();
            if (bone.has("cubes")) {
                JsonArray cubes = bone.remove("cubes").getAsJsonArray();
                BedrockModelTransform.addBones(bones, this.localChannels, bone.get("name").getAsString(), prefix,
                        cubes, BedrockModelTransform.bedrockMatrix(transformation, 1));
                break;
            }
        }
    }

    private static float modelRadius(JsonArray cubes, Matrix4fc transformation) {
        if (cubes.isEmpty()) {
            return 0;
        }
        Vector3f minimum = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f maximum = new Vector3f(Float.NEGATIVE_INFINITY);
        BedrockElementGeometry.bounds(cubes, minimum, maximum, new Matrix4f().scaling(16).mul(transformation).scale(1f / 16));
        return minimum.sub(8, 8, 8).absolute().max(maximum.sub(8, 8, 8).absolute()).length();
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

    public record Stage(BedrockItemGeometry geometry, String condition, @Nullable BedrockItemAttachables.View view) {
        // 普通阶段靠 ce_stage 选择，view 为 null；场景阶段直接绑定具体的手持或头部位置。
        public Stage(BedrockItemGeometry geometry, String condition) {
            this(geometry, condition, null);
        }
    }

    private record LocalGeometry(JsonArray cubes, Matrix4fc transformation) {
    }

    private record ContextGeometry(BedrockItemGeometry geometry, boolean posed) {
    }

    private record ContextGroup(int index, EnumSet<BedrockItemAttachables.View> views) {
    }
}
