package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.bedrock.item.equipment.Bedrock3DItemAttachables;
import net.momirealms.craftengine.core.pack.bedrock.item.equipment.BedrockWeaponGeometry;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTextures;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

final class BedrockDisplayAssets {
    private final JavaModelResources resources;
    private final Path root;
    private final String name;
    private final JsonObject textures = new JsonObject();
    private final JsonObject geometryNames = new JsonObject();
    private final JsonObject animations = new JsonObject();
    private final JsonObject animationNames = new JsonObject();
    private final JsonArray animate = new JsonArray();
    private final JsonObject controllers = new JsonObject();
    private final JsonArray activeControllers = new JsonArray();
    private final Map<Bedrock3DItemAttachables.Material, JsonArray> frames = new HashMap<>();
    private final Map<RenderPage, Map<Integer, Draw>> pages = new LinkedHashMap<>();
    private int shapes;

    BedrockDisplayAssets(JavaModelResources resources, Path root, String name) {
        this.resources = resources;
        this.root = root;
        this.name = name;
    }

    int add(List<BedrockItemDisplays.Part> parts) throws IOException {
        BedrockWeaponGeometry mesh = new BedrockWeaponGeometry(this.resources.models(), this.resources.textures());
        List<JsonArray> partsCubes = new ArrayList<>(parts.size());
        for (BedrockItemDisplays.Part part : parts) {
            ItemBindingRule rule = new ItemBindingRule(List.of(), part.target(), part.transforms(), "");
            try {
                partsCubes.add(mesh.createStage(rule, "1").cubes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (mesh.materials().isEmpty()) {
            return 0;
        }
        int shape = ++this.shapes;
        String condition = "query.property('craftengine:shape') == " + shape;
        JsonObject channels = new JsonObject();
        JsonArray geometries = new JsonArray();
        for (int layer = 0; layer < mesh.materials().size(); layer++) {
            String alias = "s" + shape + "l" + layer;
            String geometry = "geometry." + this.name + "." + alias;
            JsonArray bones = rig();
            for (int i = 0; i < parts.size(); i++) {
                JsonArray cubes = Bedrock3DItemAttachables.selectMaterialFaces(partsCubes.get(i), "layer" + layer);
                if (!cubes.isEmpty()) {
                    addPart(bones, channels, i, parts.get(i), cubes);
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
            this.addDraw(shape, layer, alias, mesh.materials().get(layer));
        }
        this.write("models/entity/" + this.name + "/s" + shape + ".geo.json", "1.21.0", "minecraft:geometry", geometries);
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

    private static void addPart(JsonArray bones, JsonObject channels, int index, BedrockItemDisplays.Part part, JsonArray cubes) {
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
        z.add("cubes", cubes);
        bones.add(x);
        bones.add(y);
        bones.add(z);
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

    private void addDraw(int shape, int layer, String alias, Bedrock3DItemAttachables.Material material) throws IOException {
        JsonArray frames = this.frames.get(material);
        if (frames == null) {
            String texture = "t" + this.frames.size();
            frames = BedrockItemTextures.writeFrames(material.data(), material.color(), texture,
                    "textures/ce/" + this.name + "/" + texture, this.textures, this.root);
            this.frames.put(material, frames);
        }
        TextureAnimation animation = material.data().animation();
        int pageSize = Math.clamp(256 / frames.size(), 1, 64);
        int start = (shape - 1) / pageSize * pageSize + 1;
        RenderPage page = new RenderPage(start, layer, frames.size(), animation == null ? 0 : animation.sampleInterval());
        this.pages.computeIfAbsent(page, ignored -> new TreeMap<>()).put(shape, new Draw(alias, frames));
    }

    private void writeControllers() {
        // 同一材质层按形状索引查表，避免每个展示实体每帧逐个判断包内所有家具的控制器。
        for (Map.Entry<RenderPage, Map<Integer, Draw>> entry : this.pages.entrySet()) {
            RenderPage page = entry.getKey();
            Map<Integer, Draw> draws = entry.getValue();
            int last = Collections.max(draws.keySet());
            JsonArray meshes = new JsonArray();
            JsonArray textures = new JsonArray();
            JsonElement fallback = draws.get(last).frames().get(0);
            for (int i = page.start(); i <= last; i++) {
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
            String index = "(query.property('craftengine:shape') - " + page.start() + ")";
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
            JsonArray selected = new JsonArray();
            selected.add("array.frames[" + sample + "]");
            controller.add("textures", selected);
            String id = "controller.render." + this.name + ".p" + this.controllers.size();
            this.controllers.add(id, controller);
            String condition = "query.property('craftengine:shape') >= " + page.start() + " && query.property('craftengine:shape') <= " + last;
            this.activeControllers.add(createObject(id, new JsonPrimitive(condition)));
        }
    }

    void finish() throws IOException {
        String emptyId = "geometry." + this.name + ".empty";
        this.geometryNames.addProperty("empty", emptyId);
        JsonObject empty = createObject("description", createObject("identifier", new JsonPrimitive(emptyId)));
        empty.add("bones", rig());
        JsonArray geometries = new JsonArray();
        geometries.add(empty);
        this.write("models/entity/" + this.name + "/empty.geo.json", "1.21.0", "minecraft:geometry", geometries);
        this.writeControllers();
        JsonObject rig = new JsonObject();
        JsonArray yaw = new JsonArray();
        yaw.add(0);
        yaw.add("query.property('craftengine:yaw')");
        yaw.add(0);
        rig.add("yaw", createObject("rotation", yaw));
        JsonArray pitch = new JsonArray();
        pitch.add("query.property('craftengine:pitch')");
        pitch.add(0);
        pitch.add(0);
        rig.add("pitch", createObject("rotation", pitch));
        rig.add("translation", propertyVector("position", "pos"));
        rig.add("left", propertyVector("rotation", "left"));
        rig.add("size", propertyVector("scale", "size"));
        rig.add("right", propertyVector("rotation", "right"));
        for (int i = 0; i < 3; i++) {
            String axis = List.of("x", "y", "z").get(i);
            JsonObject bone = i == 0 ? propertyVector("position", "model_pos") : new JsonObject();
            JsonArray angles = VectorUtils.toJson(0, 0, 0);
            angles.set(i, new JsonPrimitive("query.property('craftengine:model_rot_" + axis + "')"));
            bone.add("rotation", angles);
            if (i == 2) {
                bone.add("scale", propertyVector("scale", "model_size").get("scale"));
            }
            rig.add("model_" + axis, bone);
        }
        this.animation("pose", rig);
        this.animate.add("pose");
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "craftengine:" + this.name);
        description.add("materials", createObject("default", new JsonPrimitive("entity_alphatest")));
        description.add("textures", this.textures);
        description.add("geometry", this.geometryNames);
        description.add("animations", this.animationNames);
        description.add("scripts", createObject("animate", this.animate));
        description.add("render_controllers", this.activeControllers);
        this.write("entity/" + this.name + ".entity.json", "1.10.0", "minecraft:client_entity", createObject("description", description));
        this.write("animations/" + this.name + ".animation.json", "1.10.0", "animations", this.animations);
        this.write("render_controllers/" + this.name + ".json", "1.10.0", "render_controllers", this.controllers);
    }

    private static JsonObject propertyVector(String channel, String property) {
        JsonArray values = new JsonArray();
        for (String axis : List.of("x", "y", "z")) {
            values.add("query.property('craftengine:" + property + "_" + axis + "')");
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

    private void write(String path, String version, String key, JsonElement value) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("format_version", version);
        json.add(key, value);
        Path output = this.root.resolve(path);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(json, output);
    }

    private record RenderPage(int start, int layer, int frames, int interval) {
    }

    private record Draw(String geometry, JsonArray frames) {
    }
}
