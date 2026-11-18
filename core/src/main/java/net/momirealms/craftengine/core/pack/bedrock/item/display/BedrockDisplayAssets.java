package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockItemGeometryBuilder;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTextures;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.*;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.ANIMATION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.GEOMETRY;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

final class BedrockDisplayAssets {
    private static final double DYE_BRIGHTNESS = 0.72;
    private final JavaModelResources resources;
    private final Path root;
    private final String name;
    private final String texturePrefix;
    private final JsonObject textures = new JsonObject();
    private final JsonObject customModelDataColors = new JsonObject();
    private final JsonObject geometryNames = new JsonObject();
    private final JsonObject animations = new JsonObject();
    private final JsonObject animationNames = new JsonObject();
    private final JsonArray animate = new JsonArray();
    private final JsonObject controllers = new JsonObject();
    private final JsonArray activeControllers = new JsonArray();
    private final Map<TextureFrames, JsonArray> frames = new HashMap<>();
    private final Map<DisplayDyeMasks, Int2ObjectMap<JsonArray>> dyeMasks = new HashMap<>();
    private final Map<RenderPage, Map<Integer, Draw>> pages = new LinkedHashMap<>();
    private int shapes;

    BedrockDisplayAssets(JavaModelResources resources, Path root, String name) {
        this.resources = resources;
        this.root = root;
        this.name = name;
        this.texturePrefix = CRAFTENGINE_TEXTURES + '/' + name + '/';
    }

    int add(List<BedrockItemDisplayGenerator.Part> parts) throws IOException {
        return this.add(parts, TextureUsage.ITEM);
    }

    JsonObject customModelDataColors() {
        return this.customModelDataColors;
    }

    int add(List<BedrockItemDisplayGenerator.Part> parts, TextureUsage textureUsage) throws IOException {
        if (parts.isEmpty()) {
            // shape 0 会显示原物品；显式 empty 需要一个没有绘制控制器的独立 shape。
            return ++this.shapes;
        }
        BedrockItemGeometryBuilder mesh = new BedrockItemGeometryBuilder(this.resources.models(), this.resources.textures(), textureUsage, true);
        List<BedrockItemGeometry> partsCubes = new ArrayList<>(parts.size());
        try {
            for (BedrockItemDisplayGenerator.Part part : parts) {
                ItemBindingRule rule = new ItemBindingRule(List.of(), part.target(), part.transforms(), "");
                partsCubes.add(part.geometry() == null ? mesh.build(rule, true) : mesh.build(rule, true, part.geometry()));
            }
        } catch (BedrockResourceException exception) {
            if (!"resource_pack.bedrock.reason.tint_source".equals(exception.getMessage())) {
                throw exception;
            }
            TranslationManager.warn("resource_pack.bedrock.item.tint_fallback", parts.getFirst().target().model().asString(), "display_entity");
            mesh = new BedrockItemGeometryBuilder(this.resources.models(), this.resources.textures(), textureUsage);
            partsCubes.clear();
            for (BedrockItemDisplayGenerator.Part part : parts) {
                ItemBindingRule rule = new ItemBindingRule(List.of(), part.target(), part.transforms(), "");
                partsCubes.add(part.geometry() == null ? mesh.build(rule, false) : mesh.build(rule, false, part.geometry()));
            }
        }
        if (mesh.materials().isEmpty()) {
            return 0;
        }
        int shape = ++this.shapes;
        if (mesh.customModelDataColorIndex() >= 0) {
            this.customModelDataColors.addProperty(Integer.toString(shape), mesh.customModelDataColorIndex());
        }
        String condition = "query.property('ce:shape') == " + shape;
        JsonObject channels = new JsonObject();
        JsonArray geometries = new JsonArray();
        for (int layer = 0; layer < mesh.materials().size(); layer++) {
            String alias = "s" + shape + "l" + layer;
            String geometry = "geometry." + this.name + "." + alias;
            JsonArray bones = rig();
            for (int i = 0; i < parts.size(); i++) {
                JsonArray cubes = BedrockItemGeometry.selectMaterialFaces(partsCubes.get(i).cubes(), "layer" + layer);
                if (!cubes.isEmpty()) {
                    addPart(bones, channels, i, parts.get(i), cubes, partsCubes.get(i).transformation());
                }
            }
            JsonObject description = new JsonObject();
            description.addProperty("identifier", geometry);
            description.addProperty("texture_width", 16);
            description.addProperty("texture_height", 16);
            JsonObject file = createObject("description", description);
            file.add("bones", bones);
            geometries.add(file);
            this.geometryNames.addProperty(alias, geometry);
            BedrockItemGeometry.Material material = mesh.materials().get(layer);
            if (material.dyeColor() == null) {
                this.addDraw(shape, layer, alias, material.data(), material.color(), DyePass.NONE);
            } else {
                this.addDraw(shape, layer, alias, material.data(), material.dyeColor(), DyePass.DEFAULT);
                this.addDyeMasks(shape, layer, alias, material.data(), bones);
            }
        }
        ResourcePackUtils.writeBedrockJson(GEOMETRY, "minecraft:geometry", geometries, this.root.resolve("models/entity/" + this.name + "/s" + shape + ".geo.json"));
        if (!channels.isEmpty()) {
            String alias = "s" + shape;
            this.animation(alias, channels);
            this.animate.add(createObject(alias, new JsonPrimitive(condition)));
        }
        return shape;
    }

    private static JsonArray rig() {
        JsonArray bones = new JsonArray();
        String parent = null;
        for (String name : List.of("yaw", "pitch", "translation", "left", "size", "right", "model_x", "model_y", "model_z")) {
            bones.add(bone(name, parent));
            parent = name;
        }
        return bones;
    }

    private static JsonObject bone(String name, String parent) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        if (parent != null) {
            bone.addProperty("parent", parent);
        }
        bone.add("pivot", VectorUtils.toJson(0, 0, 0));
        return bone;
    }

    private static void addPart(JsonArray bones, JsonObject channels, int index, BedrockItemDisplayGenerator.Part part, JsonArray cubes, Matrix4fc transformation) {
        // 物品几何的中心在 (0,8,0)，展示实体的原点在 (0,0,0)。
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            for (String key : List.of("origin", "pivot")) {
                if (cube.has(key)) {
                    JsonArray xyz = cube.getAsJsonArray(key);
                    xyz.set(1, new JsonPrimitive(xyz.get(1).getAsFloat() - 8));
                }
            }
        }
        String prefix = "p" + index;
        JsonObject x = bone(prefix + "x", "model_z");
        JsonObject y = bone(prefix + "y", prefix + "x");
        JsonObject z = bone(prefix + "z", prefix + "y");
        bones.add(x);
        bones.add(y);
        bones.add(z);
        if (BedrockModelTransform.isIdentity(transformation)) {
            z.add("cubes", cubes);
        } else {
            // cubes 已向下移 8，局部矩阵也必须换到展示实体的坐标原点。
            Matrix4f local = new Matrix4f().translation(0, -8, 0)
                    .mul(BedrockModelTransform.bedrockMatrix(transformation, 1)).translate(0, 8, 0);
            BedrockModelTransform.addBones(bones, channels, prefix + "z", prefix, cubes, local);
        }
        JavaModel.Transform pose = part.display();
        if (!pose.equals(JavaModel.Transform.IDENTITY)) {
            int side = part.mirrored() ? -1 : 1;
            JsonObject first = new JsonObject();
            first.add("position", VectorUtils.toJson(-side * pose.translation().x, pose.translation().y, pose.translation().z));
            first.add("rotation", VectorUtils.toJson(-pose.rotation().x, 0, 0));
            channels.add(prefix + "x", first);
            channels.add(prefix + "y", createObject("rotation", VectorUtils.toJson(0, -side * pose.rotation().y, 0)));
            JsonObject last = createObject("rotation", VectorUtils.toJson(0, 0, side * pose.rotation().z));
            last.add("scale", VectorUtils.toJson(pose.scale()));
            channels.add(prefix + "z", last);
        }
    }

    private void addDraw(int shape, int layer, String alias, TextureData data, int color, DyePass pass) throws IOException {
        TextureFrames key = new TextureFrames(data, color);
        JsonArray frames = this.frames.get(key);
        if (frames == null) {
            String texture = "t" + this.frames.size();
            frames = BedrockItemTextures.writeFrames(data, color, texture,
                    this.texturePrefix + texture, this.textures, this.root);
            this.frames.put(key, frames);
        }
        this.addDraw(shape, layer, alias, frames, data.animation(), pass, 0);
    }

    private void addDyeMasks(int shape, int layer, String alias, TextureData data, JsonArray bones) throws IOException {
        DisplayDyeMasks source = DisplayDyeMasks.from(data, bones);
        Int2ObjectMap<JsonArray> masks = this.dyeMasks.get(source);
        if (masks == null) {
            masks = source.write("m" + this.dyeMasks.size(), this.texturePrefix, this.textures, this.root);
            this.dyeMasks.put(source, masks);
        }
        for (Int2ObjectMap.Entry<JsonArray> mask : masks.int2ObjectEntrySet()) {
            this.addDraw(shape, layer, alias, mask.getValue(), data.animation(), DyePass.DYED, mask.getIntKey());
        }
    }

    private void addDraw(int shape, int layer, String alias, JsonArray frames, TextureAnimation animation, DyePass pass, int shade) {
        int pageSize = Math.clamp(256 / frames.size(), 1, 64);
        int start = (shape - 1) / pageSize * pageSize + 1;
        RenderPage page = new RenderPage(start, layer, frames.size(), animation == null ? 0 : animation.sampleInterval(), pass, shade);
        this.pages.computeIfAbsent(page, ignored -> new TreeMap<>()).put(shape, new Draw(alias, frames));
    }

    private void writeControllers() {
        // 同一材质层按形状索引查表，避免每个展示实体每帧逐个判断包内所有家具的控制器。
        for (Map.Entry<RenderPage, Map<Integer, Draw>> entry : this.pages.entrySet()) {
            RenderPage page = entry.getKey();
            Map<Integer, Draw> draws = entry.getValue();
            int first = Collections.min(draws.keySet());
            int last = Collections.max(draws.keySet());
            JsonArray meshes = new JsonArray();
            JsonArray textures = new JsonArray();
            JsonElement fallback = draws.get(last).frames().get(0);
            for (int i = first; i <= last; i++) {
                Draw draw = draws.get(i);
                meshes.add(draw == null ? "Geometry.empty" : "Geometry." + draw.geometry());
                if (draw != null) {
                    textures.addAll(draw.frames());
                } else {
                    for (int frame = 0; frame < page.frames(); frame++) {
                        textures.add(fallback);
                    }
                }
            }
            String index = "(query.property('ce:shape') - " + first + ")";
            String sample = page.interval() == 0 ? index : index + " * " + page.frames()
                    + " + math.mod(math.floor(query.life_time * 20 / " + page.interval() + "), " + page.frames() + ")";
            JsonObject controller = new JsonObject();
            JsonObject arrays = createObject("geometries", createObject("array.meshes", meshes));
            arrays.add("textures", createObject("array.frames", textures));
            controller.add("arrays", arrays);
            controller.addProperty("geometry", "array.meshes[" + index + "]");
            JsonArray materials = new JsonArray();
            materials.add(createObject("*", new JsonPrimitive("Material.default")));
            controller.add("materials", materials);
            if (page.pass() == DyePass.DYED) {
                controller.add("overlay_color", displayColor(page.shade()));
            }
            JsonArray selected = new JsonArray();
            selected.add("array.frames[" + sample + "]");
            controller.add("textures", selected);
            String id = "controller.render." + this.name + ".p" + this.controllers.size();
            this.controllers.add(id, controller);
            String condition = shapeCondition(first, last, draws);
            if (page.pass() != DyePass.NONE) {
                condition += " && query.property('" + DISPLAY_DYE + "_r') " + (page.pass() == DyePass.DYED ? ">= 0" : "< 0");
            }
            this.activeControllers.add(createObject(id, new JsonPrimitive(condition)));
        }
    }

    private static String shapeCondition(int first, int last, Map<Integer, Draw> draws) {
        // 不启用当前形状缺少的蒙版，否则其他物品的底色也会占用绘制次数。
        String property = "query.property('ce:shape')";
        StringJoiner ranges = new StringJoiner(" || ", "(", ")");
        for (int shape = first; shape <= last; shape++) {
            if (draws.containsKey(shape)) {
                int start = shape;
                while (shape < last && draws.containsKey(shape + 1)) {
                    shape++;
                }
                ranges.add(start == shape ? property + " == " + start : property + " >= " + start + " && " + property + " <= " + shape);
            }
        }
        return ranges.toString();
    }

    void finish() throws IOException {
        String emptyId = "geometry." + this.name + ".empty";
        this.geometryNames.addProperty("empty", emptyId);
        JsonObject empty = createObject("description", createObject("identifier", new JsonPrimitive(emptyId)));
        JsonObject emptyDescription = empty.getAsJsonObject("description");
        emptyDescription.addProperty("visible_bounds_width", 4);
        emptyDescription.addProperty("visible_bounds_height", 4);
        emptyDescription.add("visible_bounds_offset", VectorUtils.toJson(0, 0, 0));
        JsonArray emptyBones = rig();
        emptyBones.add(bone("held_z", "model_z"));
        emptyBones.add(bone("held_y", "held_z"));
        emptyBones.add(bone("held_x", "held_y"));
        emptyBones.add(bone("rightitem", "held_x"));
        empty.add("bones", emptyBones);
        JsonArray geometries = new JsonArray();
        geometries.add(empty);
        ResourcePackUtils.writeBedrockJson(GEOMETRY, "minecraft:geometry", geometries, this.root.resolve("models/entity/" + this.name + "/empty.geo.json"));
        this.writeControllers();
        JsonObject held = new JsonObject();
        held.addProperty("geometry", "Geometry.empty");
        JsonArray heldMaterials = new JsonArray();
        heldMaterials.add(createObject("*", new JsonPrimitive("Material.default")));
        held.add("materials", heldMaterials);
        JsonArray heldTextures = new JsonArray();
        heldTextures.add("Texture.default");
        held.add("textures", heldTextures);
        this.textures.addProperty("default", "textures/items/apple");
        String heldController = "controller.render." + this.name + ".held";
        this.controllers.add(heldController, held);
        this.activeControllers.add(createObject(heldController, new JsonPrimitive("query.property('ce:shape') == 0")));
        JsonObject rig = new JsonObject();
        JsonArray yaw = new JsonArray();
        yaw.add(0);
        yaw.add("query.property('ce:yaw')");
        yaw.add(0);
        rig.add("yaw", createObject("rotation", yaw));
        JsonArray pitch = new JsonArray();
        pitch.add("query.property('ce:pitch')");
        pitch.add(0);
        pitch.add(0);
        rig.add("pitch", createObject("rotation", pitch));
        rig.add("translation", interpolatedVector("position", "pos"));
        rig.add("left", interpolatedVector("rotation", "left"));
        rig.add("size", interpolatedVector("scale", "size"));
        rig.add("right", interpolatedVector("rotation", "right"));
        for (int i = 0; i < 3; i++) {
            String axis = List.of("x", "y", "z").get(i);
            JsonObject bone = i == 0 ? propertyVector("position", "model_pos") : new JsonObject();
            JsonArray angles = VectorUtils.toJson(0, 0, 0);
            angles.set(i, new JsonPrimitive("query.property('ce:model_rot_" + axis + "')"));
            bone.add("rotation", angles);
            if (i == 2) {
                bone.add("scale", propertyVector("scale", "model_size").get("scale"));
            }
            rig.add("model_" + axis, bone);
        }
        this.animation("pose", rig);
        this.animate.add("pose");
        JsonObject description = new JsonObject();
        description.addProperty("identifier", ID_PREFIX + this.name);
        description.addProperty("enable_attachables", true);
        JsonObject materials = createObject("default", new JsonPrimitive("entity_alphatest_one_sided"));
        description.add("materials", materials);
        description.add("textures", this.textures);
        description.add("geometry", this.geometryNames);
        description.add("animations", this.animationNames);
        description.add("scripts", BedrockDisplayInterpolation.scripts(this.animate));
        description.add("render_controllers", this.activeControllers);
        ResourcePackUtils.writeBedrockJson(CLIENT_ENTITY, "minecraft:client_entity", createObject("description", description), this.root.resolve("entity/" + this.name + ".entity.json"));
        JsonObject heldDescription = new JsonObject();
        heldDescription.addProperty("identifier", ID_PREFIX + this.name + "_held");
        heldDescription.addProperty("enable_attachables", true);
        heldDescription.add("materials", materials);
        heldDescription.add("textures", createObject("default", new JsonPrimitive("textures/items/apple")));
        // 普通手持物品需要单几何载体，不能与 CE 模型的多几何渲染控制器共用实体定义。
        heldDescription.add("geometry", createObject("default", new JsonPrimitive(emptyId)));
        JsonObject heldAnimations = createObject("pose", this.animationNames.get("pose"));
        heldDescription.add("animations", heldAnimations);
        JsonArray heldAnimate = new JsonArray();
        heldAnimate.add("pose");
        for (BedrockHeldItemType type : BedrockHeldItemType.values()) {
            JavaModel.Transform correction = BedrockHeldItemTransforms.correction(type);
            JsonObject channels = new JsonObject();
            JsonObject z = createObject("position", VectorUtils.toJson(correction.translation()));
            z.add("rotation", VectorUtils.toJson(0, 0, correction.rotation().z));
            channels.add("held_z", z);
            channels.add("held_y", createObject("rotation", VectorUtils.toJson(0, correction.rotation().y, 0)));
            JsonObject x = createObject("rotation", VectorUtils.toJson(correction.rotation().x, 0, 0));
            x.add("scale", VectorUtils.toJson(correction.scale()));
            channels.add("held_x", x);
            String alias = "held_" + type.id();
            this.animation(alias, channels);
            heldAnimations.add(alias, this.animationNames.get(alias));
            heldAnimate.add(createObject(alias, new JsonPrimitive("query.property('" + BedrockHeldItemType.PROPERTY + "') == " + type.id())));
        }
        heldDescription.add("scripts", BedrockDisplayInterpolation.scripts(heldAnimate));
        JsonArray heldControllers = new JsonArray();
        heldControllers.add("controller.render.default");
        heldDescription.add("render_controllers", heldControllers);
        ResourcePackUtils.writeBedrockJson(CLIENT_ENTITY, "minecraft:client_entity", createObject("description", heldDescription), this.root.resolve("entity/" + this.name + "_held.entity.json"));
        ResourcePackUtils.writeBedrockJson(ANIMATION, "animations", this.animations, this.root.resolve("animations/" + this.name + ".animation.json"));
        ResourcePackUtils.writeBedrockJson(RENDER_CONTROLLER, "render_controllers", this.controllers, this.root.resolve("render_controllers/" + this.name + ".json"));
    }

    private static JsonObject interpolatedVector(String channel, String group) {
        JsonArray values = new JsonArray();
        for (String axis : List.of("x", "y", "z")) {
            values.add("v.ce_" + group + "_" + (channel.equals("rotation") ? "r" : "") + axis);
        }
        return createObject(channel, values);
    }

    private static JsonObject propertyVector(String channel, String property) {
        JsonArray values = new JsonArray();
        for (String axis : List.of("x", "y", "z")) {
            values.add("query.property('ce:" + property + "_" + axis + "')");
        }
        return createObject(channel, values);
    }

    private void animation(String alias, JsonObject bones) {
        String id = "animation." + this.name + "." + alias;
        JsonObject animation = createObject("bones", bones);
        animation.addProperty("loop", true);
        this.animationNames.addProperty(alias, id);
        this.animations.add(id, animation);
    }

    private static JsonObject displayColor(int shade) {
        String gb = "query.property('" + DISPLAY_DYE + "_gb')";
        JsonObject color = new JsonObject();
        // 每张蒙版只覆盖一种底色。基岩叠色比预染贴图亮，使用实测系数补偿。
        color.addProperty("r", "query.property('" + DISPLAY_DYE + "_r') / 255 * " + (shade >> 16) + " / 255 * " + DYE_BRIGHTNESS);
        color.addProperty("g", "math.floor(" + gb + " / 256) / 255 * " + (shade >> 8 & 255) + " / 255 * " + DYE_BRIGHTNESS);
        color.addProperty("b", "math.mod(" + gb + ", 256) / 255 * " + (shade & 255) + " / 255 * " + DYE_BRIGHTNESS);
        color.addProperty("a", 1);
        return color;
    }

    private enum DyePass {
        NONE, DEFAULT, DYED
    }

    private record TextureFrames(TextureData data, int color) {
    }

    private record RenderPage(int start, int layer, int frames, int interval, DyePass pass, int shade) {
    }

    private record Draw(String geometry, JsonArray frames) {
    }
}
