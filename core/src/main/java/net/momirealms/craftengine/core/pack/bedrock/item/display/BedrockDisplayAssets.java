package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.bedrock.BedrockTintSource;
import net.momirealms.craftengine.core.pack.bedrock.BedrockEntityDye;
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
    private static final String SHAPE_VARIABLE = "v.ce_shape";
    private static final String VISIBLE = "!v.ce_hidden";
    private static final String[] AXES = { "x", "y", "z" };
    private static final String[] RIG_BONES = { "yaw", "pitch", "translation", "left", "size", "right", "model_x", "model_y" };
    private static final JsonArray RIG = createRig();
    private static final String[] CUBE_POINTS = { "origin", "pivot" };
    private final JavaModelResources resources;
    private final Path root;
    private final String name;
    private final String texturePrefix;
    private final JsonObject textures = new JsonObject();
    private final JsonObject tintSources = new JsonObject();
    private final JsonObject geometryNames = new JsonObject();
    private final JsonObject animations = new JsonObject();
    private final JsonObject animationNames = new JsonObject();
    private final JsonArray animate = new JsonArray();
    private final JsonObject controllers = new JsonObject();
    private final JsonArray activeControllers = new JsonArray();
    private final Map<TextureFrames, JsonArray> frames = new HashMap<>();
    private final Map<MaskKey, Int2ObjectMap<JsonArray>> dyeMasks = new HashMap<>();
    private final Map<RenderPage, Map<Integer, Draw>> pages = new LinkedHashMap<>();
    private int shapes;

    BedrockDisplayAssets(JavaModelResources resources, Path root, String name) {
        this.resources = resources;
        this.root = root;
        this.name = name;
        this.texturePrefix = CRAFTENGINE_TEXTURES + '/' + name + '/';
    }

    JsonObject tintSources() {
        return this.tintSources;
    }

    int add(List<BedrockItemDisplayGenerator.Part> parts, TextureUsage textureUsage, BedrockEntityDye dye) throws IOException {
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
        List<BedrockItemGeometry.Material> materials = mesh.materials();
        if (materials.isEmpty()) {
            return 0;
        }
        List<Map<String, JsonArray>> layersByPart = new ArrayList<>(partsCubes.size());
        for (BedrockItemGeometry geometry : partsCubes) {
            layersByPart.add(geometry.splitMaterialFaces());
        }
        int shape = ++this.shapes;
        if (BedrockTintSource.isDynamic(mesh.tintSource()) && mesh.tintSource() != BedrockTintSource.DYED_COLOR) {
            this.tintSources.addProperty(Integer.toString(shape), mesh.tintSource());
        }
        String condition = SHAPE_VARIABLE + " == " + shape;
        JsonObject channels = new JsonObject();
        JsonArray geometries = new JsonArray(materials.size());
        for (int layer = 0; layer < materials.size(); layer++) {
            String alias = "s" + shape + "l" + layer;
            String geometry = "geometry." + this.name + "." + alias;
            String materialId = "layer" + layer;
            JsonObject modelRoot = bone("model_z", "model_y");
            JsonArray bones = rig(modelRoot);
            for (int i = 0; i < parts.size(); i++) {
                BedrockItemGeometry partGeometry = partsCubes.get(i);
                JsonArray cubes = layersByPart.get(i).get(materialId);
                if (cubes != null) {
                    addPart(bones, modelRoot, channels, i, parts.get(i), cubes, partGeometry.transformation());
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
            BedrockItemGeometry.Material material = materials.get(layer);
            if (material.dyeColor() == null) {
                this.addDraw(shape, layer, alias, material.data(), material.color(), "");
            } else {
                this.addDraw(shape, layer, alias, material.data(), material.dyeColor(), dye.baseCondition(DISPLAY_DYE, false));
                for (int rgb : dye.prebakedColors()) {
                    this.addDraw(shape, layer, alias, material.data(), rgb, BedrockEntityDye.matches(DISPLAY_DYE, rgb));
                }
                if (dye.maxLayers() > 0) {
                    this.addDyeMasks(shape, layer, alias, material.data(), bones, dye);
                }
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

    private static JsonArray rig(JsonObject modelRoot) {
        JsonArray bones = new JsonArray(RIG.size() + 4);
        bones.addAll(RIG);
        bones.add(modelRoot);
        return bones;
    }

    private static JsonArray createRig() {
        JsonArray bones = new JsonArray(RIG_BONES.length);
        String parent = null;
        for (String name : RIG_BONES) {
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

    private static void addPart(JsonArray bones, JsonObject modelRoot, JsonObject channels, int index, BedrockItemDisplayGenerator.Part part, JsonArray cubes, Matrix4fc transformation) {
        // 物品几何的中心在 (0,8,0)，展示实体的原点在 (0,0,0)。
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            for (String key : CUBE_POINTS) {
                JsonArray xyz = cube.getAsJsonArray(key);
                if (xyz != null) {
                    xyz.set(1, new JsonPrimitive(xyz.get(1).getAsFloat() - 8));
                }
            }
        }
        boolean identityMatrix = BedrockModelTransform.isIdentity(transformation);
        JavaModel.Transform pose = part.display();
        boolean identityPose = pose.equals(JavaModel.Transform.IDENTITY);
        if (identityMatrix && identityPose) {
            JsonObject parent = modelRoot;
            // 已有带变换的子骨骼时，保留一个同级节点，让重叠面仍按原部件顺序绘制。
            if (bones.get(bones.size() - 1) != modelRoot) {
                parent = bone("p" + index + "z", "model_z");
                bones.add(parent);
            }
            JsonArray existing = parent.getAsJsonArray("cubes");
            if (existing == null) {
                parent.add("cubes", cubes);
            } else {
                existing.addAll(cubes);
            }
            return;
        }
        String prefix = "p" + index;
        JsonObject x = bone(prefix + "x", "model_z");
        JsonObject y = bone(prefix + "y", prefix + "x");
        JsonObject z = bone(prefix + "z", prefix + "y");
        bones.add(x);
        bones.add(y);
        bones.add(z);
        if (identityMatrix) {
            z.add("cubes", cubes);
        } else {
            // cubes 已向下移 8，局部矩阵也必须换到展示实体的坐标原点。
            Matrix4f local = new Matrix4f().translation(0, -8, 0)
                    .mul(BedrockModelTransform.bedrockMatrix(transformation, 1)).translate(0, 8, 0);
            BedrockModelTransform.addBones(bones, channels, prefix + "z", prefix, cubes, local);
        }
        if (!identityPose) {
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

    private void addDraw(int shape, int layer, String alias, TextureData data, int color, String condition) throws IOException {
        TextureFrames key = new TextureFrames(data, color);
        JsonArray frames = this.frames.get(key);
        if (frames == null) {
            String texture = "t" + this.frames.size();
            frames = BedrockItemTextures.writeFrames(data, color, texture,
                    this.texturePrefix + texture, this.textures, this.root);
            this.frames.put(key, frames);
        }
        this.addDraw(shape, layer, alias, frames, data.animation(), condition, -1);
    }

    private void addDyeMasks(int shape, int layer, String alias, TextureData data, JsonArray bones, BedrockEntityDye dye) throws IOException {
        DisplayDyeMasks source = DisplayDyeMasks.from(data, bones);
        MaskKey key = new MaskKey(source, dye.maxLayers());
        Int2ObjectMap<JsonArray> masks = this.dyeMasks.get(key);
        if (masks == null) {
            masks = source.write("m" + this.dyeMasks.size(), this.texturePrefix, this.textures, this.root, dye.maxLayers());
            this.dyeMasks.put(key, masks);
        }
        for (Int2ObjectMap.Entry<JsonArray> mask : masks.int2ObjectEntrySet()) {
            this.addDraw(shape, layer, alias, mask.getValue(), data.animation(), dye.dynamicCondition(DISPLAY_DYE, false), mask.getIntKey());
        }
    }

    private void addDraw(int shape, int layer, String alias, JsonArray frames, TextureAnimation animation, String dyeCondition, int shade) {
        int pageSize = Math.clamp(256 / frames.size(), 1, 64);
        int start = (shape - 1) / pageSize * pageSize + 1;
        RenderPage page = new RenderPage(start, layer, frames.size(), animation == null ? 0 : animation.sampleInterval(), dyeCondition, shade);
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
            Draw fallback = draws.get(last);
            int gapEnd = first;
            int gapSize = 0;
            int previous = first;
            for (int shape : draws.keySet()) {
                int skipped = shape - previous - 1;
                if (skipped > gapSize) {
                    gapSize = skipped;
                    gapEnd = shape;
                }
                previous = shape;
            }
            // 稀疏页只折叠最大的空段；可见性条件已排除空槽，至多增加一次索引修正。
            boolean compact = gapSize * (page.frames() + 1) >= 8;
            for (int i = first; i <= last; i++) {
                if (compact && i == gapEnd - gapSize) {
                    i = gapEnd;
                }
                Draw draw = draws.getOrDefault(i, fallback);
                meshes.add("Geometry." + draw.geometry());
                textures.addAll(draw.frames());
            }
            String index = "(" + SHAPE_VARIABLE + " - " + first + (compact ? " - (" + SHAPE_VARIABLE + " >= " + gapEnd + " ? " + gapSize + " : 0)" : "") + ")";
            String sample = page.interval() == 0 ? index : index + " * " + page.frames() + " + math.mod(math.floor(query.life_time * 20 / " + page.interval() + "), " + page.frames() + ")";
            JsonObject controller = new JsonObject();
            controller.addProperty("geometry", ResourcePackUtils.selectBedrockResource(controller, "geometries", "array.meshes", meshes, index));
            JsonArray materials = new JsonArray();
            materials.add(createObject("*", new JsonPrimitive("Material.default")));
            controller.add("materials", materials);
            if (page.shade() >= 0) {
                controller.add("overlay_color", displayColor(page.shade()));
            }
            JsonArray selected = new JsonArray();
            selected.add(ResourcePackUtils.selectBedrockResource(controller, "textures", "array.frames", textures, sample));
            controller.add("textures", selected);
            String id = "controller.render." + this.name + ".p" + this.controllers.size();
            this.controllers.add(id, controller);
            String condition = VISIBLE + " && " + shapeCondition(first, last, draws);
            if (!page.dyeCondition().isEmpty()) {
                condition += " && (" + page.dyeCondition() + ")";
            }
            this.activeControllers.add(createObject(id, new JsonPrimitive(condition)));
        }
    }

    private static String shapeCondition(int first, int last, Map<Integer, Draw> draws) {
        StringJoiner present = new StringJoiner(" || ", "(", ")");
        StringJoiner exceptGaps = new StringJoiner(" && ", "(", ")");
        exceptGaps.add(shapeRange(first, last, true));
        // 同一页可以列出有绘制的区间，也可以在页边界内排除空洞。两种条件选较短的一个。
        for (int start = first; start <= last;) {
            boolean included = draws.containsKey(start);
            int end = start;
            while (end < last && draws.containsKey(end + 1) == included) {
                end++;
            }
            (included ? present : exceptGaps).add(shapeRange(start, end, included));
            start = end + 1;
        }
        return present.length() <= exceptGaps.length() ? present.toString() : exceptGaps.toString();
    }

    private static String shapeRange(int first, int last, boolean included) {
        if (first == last) {
            return SHAPE_VARIABLE + (included ? " == " : " != ") + first;
        }
        if (included) {
            return SHAPE_VARIABLE + " >= " + first + " && " + SHAPE_VARIABLE + " <= " + last;
        }
        return "(" + SHAPE_VARIABLE + " < " + first + " || " + SHAPE_VARIABLE + " > " + last + ")";
    }

    void finish() throws IOException {
        String emptyId = "geometry." + this.name + ".empty";
        this.geometryNames.addProperty("empty", emptyId);
        JsonObject empty = createObject("description", createObject("identifier", new JsonPrimitive(emptyId)));
        JsonObject emptyDescription = empty.getAsJsonObject("description");
        emptyDescription.addProperty("visible_bounds_width", 4);
        emptyDescription.addProperty("visible_bounds_height", 4);
        emptyDescription.add("visible_bounds_offset", VectorUtils.toJson(0, 0, 0));
        JsonArray emptyBones = rig(bone("model_z", "model_y"));
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
        this.activeControllers.add(createObject(heldController, new JsonPrimitive(VISIBLE + " && " + SHAPE_VARIABLE + " == 0")));
        JsonObject rig = new JsonObject();
        JsonArray yaw = new JsonArray();
        yaw.add(0);
        // 载体的偏航已应用到乘客，骨骼只需补上从载体朝向到目标朝向的差值
        yaw.add("((v.ce_billboard == 1 || v.ce_billboard == 3) ? q.camera_rotation(1) - 180 : q.property('ce:yaw')) - q.ride_body_y_rotation");
        yaw.add(0);
        rig.add("yaw", createObject("rotation", yaw));
        JsonArray pitch = new JsonArray();
        pitch.add("v.ce_billboard >= 2 ? -q.camera_rotation(0) : q.property('ce:pitch')");
        pitch.add(0);
        pitch.add(0);
        rig.add("pitch", createObject("rotation", pitch));
        rig.add("translation", interpolatedVector("position", "pos"));
        rig.add("left", interpolatedVector("rotation", "left"));
        JsonArray size = new JsonArray(AXES.length);
        for (String axis : AXES) {
            size.add("v.ce_hidden ? 0 : v.ce_size_" + axis);
        }
        rig.add("size", createObject("scale", size));
        rig.add("right", interpolatedVector("rotation", "right"));
        for (int i = 0; i < AXES.length; i++) {
            String axis = AXES[i];
            JsonObject bone = i == 0 ? propertyVector("position", "model_pos") : new JsonObject();
            JsonArray angles = VectorUtils.toJson(0, 0, 0);
            angles.set(i, new JsonPrimitive("query.property('ce:model_rot_" + axis + "')"));
            bone.add("rotation", angles);
            if (i == AXES.length - 1) {
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
        JsonObject scripts = BedrockDisplayInterpolation.scripts(this.animate);
        scripts.getAsJsonArray("pre_animation").add(SHAPE_VARIABLE + " = q.property('ce:shape');");
        description.add("scripts", scripts);
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
            heldAnimate.add(createObject(alias, new JsonPrimitive("v.ce_held_type == " + type.id())));
        }
        heldDescription.add("scripts", BedrockDisplayInterpolation.scripts(heldAnimate));
        JsonArray heldControllers = new JsonArray();
        heldControllers.add(createObject("controller.render.default", new JsonPrimitive(VISIBLE)));
        heldDescription.add("render_controllers", heldControllers);
        ResourcePackUtils.writeBedrockJson(CLIENT_ENTITY, "minecraft:client_entity", createObject("description", heldDescription), this.root.resolve("entity/" + this.name + "_held.entity.json"));
        ResourcePackUtils.writeBedrockJson(ANIMATION, "animations", this.animations, this.root.resolve("animations/" + this.name + ".animation.json"));
        ResourcePackUtils.writeBedrockJson(RENDER_CONTROLLER, "render_controllers", this.controllers, this.root.resolve("render_controllers/" + this.name + ".json"));
    }

    private static JsonObject interpolatedVector(String channel, String group) {
        JsonArray values = new JsonArray(AXES.length);
        String prefix = "v.ce_" + group + "_" + (channel.equals("rotation") ? "r" : "");
        for (String axis : AXES) {
            values.add(prefix + axis);
        }
        return createObject(channel, values);
    }

    private static JsonObject propertyVector(String channel, String property) {
        JsonArray values = new JsonArray(AXES.length);
        for (String axis : AXES) {
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

    private record MaskKey(DisplayDyeMasks masks, int maxLayers) {
    }

    private record TextureFrames(TextureData data, int color) {
    }

    private record RenderPage(int start, int layer, int frames, int interval, String dyeCondition, int shade) {
    }

    private record Draw(String geometry, JsonArray frames) {
    }
}
