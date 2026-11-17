package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.item.display.DisplayDyeMasks;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.GsonHelper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NativeModelAssets {
    public static final String MANIFEST = BedrockPackLayout.NATIVE_MODELS;
    private final JsonObject blueprint;
    private final String stem;
    private final Map<String, JsonObject> elements = new HashMap<>();
    private final Map<String, JsonObject> groups = new HashMap<>();
    private final Map<String, String> boneIds = new HashMap<>();
    private final Map<String, String> boneNames = new LinkedHashMap<>();
    private final List<Texture> textures = new ArrayList<>();
    private final JsonArray bones = new JsonArray();
    private BufferedImage atlas;

    private NativeModelAssets(NativeModelSource source) throws IOException {
        this.blueprint = GsonHelper.readJsonObjectFromFile(source.blueprint());
        this.stem = UUID.nameUUIDFromBytes(source.id().asString().getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    public static void write(List<NativeModelSource> sources, Path root) throws IOException {
        write(sources, root, 16);
    }

    public static void write(List<NativeModelSource> sources, Path root, int maxLayers) throws IOException {
        write(sources, root, root.resolve(MANIFEST), maxLayers);
    }

    public static void write(List<NativeModelSource> sources, Path root, Path modelData, int maxLayers) throws IOException {
        JsonObject models = new JsonObject();
        for (NativeModelSource source : sources) {
            try {
                String material = switch (source.id().namespace()) {
                    case "bettermodel", "modelengine" -> "entity_alphatest_one_sided";
                    default -> "entity_alphatest";
                };
                models.add(source.id().asString(), new NativeModelAssets(source).write(root, maxLayers, material));
            } catch (RuntimeException exception) {
                throw new IOException("Cannot convert native model " + source.id() + " from " + source.blueprint(), exception);
            }
        }
        Files.createDirectories(modelData.getParent());
        GsonHelper.writeJsonFile(models, modelData);
    }

    private JsonObject write(Path root, int maxLayers, String material) throws IOException {
        int[] size = this.writeTexture(root);
        for (JsonElement element : this.blueprint.getAsJsonArray("elements")) {
            JsonObject cube = element.getAsJsonObject();
            if (cube.has("type") && List.of("bounding_box", "locator", "null_object", "camera").contains(cube.get("type").getAsString())) {
                continue;
            }
            if (cube.has("type") && !cube.get("type").getAsString().equals("cube")) {
                throw new IllegalArgumentException("Unsupported native model element: " + cube.get("type"));
            }
            this.elements.put(cube.get("uuid").getAsString(), cube);
        }
        if (this.blueprint.has("groups")) {
            for (JsonElement element : this.blueprint.getAsJsonArray("groups")) {
                JsonObject group = element.getAsJsonObject();
                this.groups.put(group.get("uuid").getAsString(), group);
            }
        }
        JsonObject rootBone = new JsonObject();
        rootBone.addProperty("name", "root");
        rootBone.add("pivot", vector(0, 0, 0));
        rootBone.add("cubes", new JsonArray());
        this.bones.add(rootBone);
        for (JsonElement node : this.blueprint.getAsJsonArray("outliner")) {
            this.addNode(node, rootBone);
        }
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.ce_native_" + this.stem);
        description.addProperty("texture_width", size[0]);
        description.addProperty("texture_height", size[1]);
        description.addProperty("visible_bounds_width", 64);
        description.addProperty("visible_bounds_height", 64);
        description.add("visible_bounds_offset", vector(0, 0, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", this.bones);
        JsonObject geometries = JsonParser.parseString("{\"format_version\":\"1.21.0\",\"minecraft:geometry\":[]}").getAsJsonObject();
        geometries.getAsJsonArray("minecraft:geometry").add(geometry);
        this.save(root, "models/entity/craftengine/native/", ".geo.json", geometries);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("identifier", "craftengine:native_" + this.stem);
        JsonObject names = new JsonObject();
        this.boneNames.forEach(names::addProperty);
        manifest.add("bones", names);
        JsonArray animationNames = new JsonArray();
        manifest.add("animations", animationNames);
        JsonObject animations = new JsonObject();
        JsonObject controllers = new JsonObject();
        JsonObject aliases = new JsonObject();
        JsonArray animate = new JsonArray();
        if (this.blueprint.has("animations")) {
            for (JsonElement element : this.blueprint.getAsJsonArray("animations")) {
                JsonObject animation = element.getAsJsonObject();
                int index = animationNames.size();
                animationNames.add(animation.get("name").getAsString());
                String alias = "a" + index;
                String id = "animation.ce_native_" + this.stem + "." + alias;
                JsonObject converted = this.animation(animation);
                String controllerId = "controller.animation.ce_native_" + this.stem + "." + alias;
                JsonObject controller = JsonParser.parseString("{\"initial_state\":\"stop\",\"states\":{\"stop\":{\"transitions\":[]}}}").getAsJsonObject();
                JsonObject states = controller.getAsJsonObject("states");
                String property = "math.mod(math.floor(q.property('craftengine:model_anim" + index / 9 + "') / " + (1 << (index % 9 * 2)) + "), 4)";
                for (int mode = 1; mode <= 3; mode++) {
                    String variant = alias + "_" + mode;
                    JsonObject clip = converted.deepCopy();
                    if (mode == 3) {
                        clip.addProperty("loop", "hold_on_last_frame");
                    } else {
                        clip.addProperty("loop", mode == 2);
                    }
                    animations.add(id + "_" + mode, clip);
                    aliases.addProperty(variant, id + "_" + mode);
                    JsonObject transition = new JsonObject();
                    transition.addProperty(variant, property + " == " + mode);
                    states.getAsJsonObject("stop").getAsJsonArray("transitions").add(transition);
                    JsonObject play = JsonParser.parseString("{\"animations\":[],\"transitions\":[]}").getAsJsonObject();
                    play.getAsJsonArray("animations").add(variant);
                    JsonObject stop = new JsonObject();
                    stop.addProperty("stop", property + " != " + mode);
                    play.getAsJsonArray("transitions").add(stop);
                    states.add(variant, play);
                }
                controllers.add(controllerId, controller);
                aliases.addProperty("c" + index, controllerId);
                animate.add("c" + index);
            }
        }
        JsonObject scaleAnimation = JsonParser.parseString("{\"loop\":true,\"bones\":{\"root\":{\"scale\":[\"q.property('craftengine:model_sx')\",\"q.property('craftengine:model_sy')\",\"q.property('craftengine:model_sz')\"]}}}").getAsJsonObject();
        animations.add("animation.ce_native_" + this.stem + ".scale", scaleAnimation);
        aliases.addProperty("scale", "animation.ce_native_" + this.stem + ".scale");
        animate.add("scale");
        JsonObject animationFile = new JsonObject();
        animationFile.addProperty("format_version", "1.8.0");
        animationFile.add("animations", animations);
        this.save(root, "animations/craftengine/native/", ".json", animationFile);
        JsonObject controllerFile = new JsonObject();
        controllerFile.addProperty("format_version", "1.10.0");
        controllerFile.add("animation_controllers", controllers);
        this.save(root, "animation_controllers/craftengine/native/", ".json", controllerFile);

        JsonObject render = JsonParser.parseString("{\"geometry\":\"Geometry.default\",\"materials\":[{\"*\":\"Material.default\"}],\"textures\":[\"Texture.default\"],\"part_visibility\":[]}").getAsJsonObject();
        int boneIndex = 0;
        for (String name : this.boneNames.values()) {
            JsonObject visible = new JsonObject();
            visible.addProperty(name, bit("bone", boneIndex++) + " == 0");
            render.getAsJsonArray("part_visibility").add(visible);
        }
        JsonObject color = JsonParser.parseString("{\"r\":\"q.property('craftengine:model_dye_r') < 0 ? 1 : q.property('craftengine:model_dye_r') / 255\",\"g\":\"q.property('craftengine:model_dye_r') < 0 ? 1 : math.floor(q.property('craftengine:model_dye_gb') / 256) / 255\",\"b\":\"q.property('craftengine:model_dye_r') < 0 ? 1 : math.mod(q.property('craftengine:model_dye_gb'), 256) / 255\",\"a\":1}").getAsJsonObject();
        JsonObject renderFile = JsonParser.parseString("{\"format_version\":\"1.8.0\",\"render_controllers\":{}}").getAsJsonObject();
        String renderId = "controller.render.ce_native_" + this.stem;
        renderFile.getAsJsonObject("render_controllers").add(renderId, render);
        JsonObject entity = JsonParser.parseString("{\"format_version\":\"1.10.0\",\"minecraft:client_entity\":{\"description\":{\"materials\":{},\"textures\":{},\"geometry\":{},\"scripts\":{},\"render_controllers\":[]}}}").getAsJsonObject();
        JsonObject entityDescription = entity.getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        entityDescription.getAsJsonObject("materials").addProperty("default", material);
        entityDescription.add("identifier", manifest.get("identifier"));
        entityDescription.getAsJsonObject("textures").addProperty("default", "textures/ce_native/" + this.stem);
        entityDescription.getAsJsonObject("geometry").addProperty("default", "geometry.ce_native_" + this.stem);
        entityDescription.add("animations", aliases);
        entityDescription.getAsJsonObject("scripts").add("animate", animate);
        String defaultColor = "q.property('craftengine:model_dye_r') < 0 || (q.property('craftengine:model_dye_r') == 255 && q.property('craftengine:model_dye_gb') == 65535)";
        JsonObject defaultController = new JsonObject();
        defaultController.addProperty(renderId, defaultColor);
        entityDescription.getAsJsonArray("render_controllers").add(defaultController);
        BitSet coverage = new BitSet(this.atlas.getWidth() * this.atlas.getHeight());
        coverage.set(0, this.atlas.getWidth() * this.atlas.getHeight());
        DisplayDyeMasks masks = new DisplayDyeMasks(new TextureData(this.atlas, null), coverage);
        Int2ObjectMap<JsonArray> layers = masks.write("dye", "textures/ce_native/" + this.stem + "/", entityDescription.getAsJsonObject("textures"), root, maxLayers);
        for (Int2ObjectMap.Entry<JsonArray> layer : layers.int2ObjectEntrySet()) {
            JsonObject tinted = render.deepCopy();
            tinted.add("textures", layer.getValue());
            JsonObject overlay = color.deepCopy();
            int shade = layer.getIntKey();
            overlay.addProperty("r", "(" + color.get("r").getAsString() + ") * " + (shade >> 16) + " / 255 * 0.72");
            overlay.addProperty("g", "(" + color.get("g").getAsString() + ") * " + (shade >> 8 & 255) + " / 255 * 0.72");
            overlay.addProperty("b", "(" + color.get("b").getAsString() + ") * " + (shade & 255) + " / 255 * 0.72");
            tinted.add("overlay_color", overlay);
            String tintedId = renderId + ".d" + Integer.toHexString(shade);
            renderFile.getAsJsonObject("render_controllers").add(tintedId, tinted);
            JsonObject condition = new JsonObject();
            condition.addProperty(tintedId, "!(" + defaultColor + ")");
            entityDescription.getAsJsonArray("render_controllers").add(condition);
        }
        this.save(root, "render_controllers/craftengine/native/", ".json", renderFile);
        this.save(root, "entity/craftengine/native/", ".entity.json", entity);
        return manifest;
    }

    private int[] writeTexture(Path root) throws IOException {
        int width = 1;
        int height = 0;
        JsonObject resolution = this.blueprint.getAsJsonObject("resolution");
        for (JsonElement element : this.blueprint.getAsJsonArray("textures")) {
            JsonObject texture = element.getAsJsonObject();
            String source = texture.get("source").getAsString();
            if (!source.startsWith("data:image/png;base64,")) {
                throw new IOException("Native model textures must be embedded PNGs");
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1))));
            if (image == null) {
                throw new IOException("Invalid native model texture");
            }
            double uvWidth = texture.has("uv_width") ? texture.get("uv_width").getAsDouble() : resolution.get("width").getAsDouble();
            double uvHeight = texture.has("uv_height") ? texture.get("uv_height").getAsDouble() : resolution.get("height").getAsDouble();
            this.textures.add(new Texture(image, height, image.getWidth() / uvWidth, image.getHeight() / uvHeight));
            width = Math.max(width, image.getWidth());
            height += image.getHeight();
        }
        BufferedImage atlas = new BufferedImage(width, Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        this.atlas = atlas;
        Graphics2D graphics = atlas.createGraphics();
        try {
            for (Texture texture : this.textures) {
                graphics.drawImage(texture.image(), 0, texture.offset(), null);
            }
        } finally {
            graphics.dispose();
        }
        Path output = root.resolve("textures/ce_native/" + this.stem + ".png");
        Files.createDirectories(output.getParent());
        try (java.io.OutputStream stream = Files.newOutputStream(output)) {
            ImageIO.write(atlas, "png", stream);
        }
        return new int[]{atlas.getWidth(), atlas.getHeight()};
    }

    private void addNode(JsonElement node, JsonObject parent) {
        if (node.isJsonPrimitive()) {
            JsonObject cube = this.elements.get(node.getAsString());
            if (cube != null && (!cube.has("visibility") || cube.get("visibility").getAsBoolean())) {
                parent.getAsJsonArray("cubes").add(this.cube(cube));
            }
            return;
        }
        JsonObject outline = node.getAsJsonObject();
        String uuid = outline.get("uuid").getAsString();
        JsonObject group = this.groups.getOrDefault(uuid, outline);
        String name = group.get("name").getAsString();
        String boneId = "b" + this.boneNames.size();
        if (this.boneNames.putIfAbsent(name, boneId) != null) {
            throw new IllegalArgumentException("Duplicate bone name: " + name);
        }
        this.boneIds.put(uuid, boneId);
        JsonObject bone = new JsonObject();
        bone.addProperty("name", boneId);
        bone.add("parent", parent.get("name"));
        bone.add("pivot", transformed(group.getAsJsonArray("origin"), -1, 1, 1, 0));
        bone.add("rotation", transformed(group.getAsJsonArray("rotation"), -1, -1, 1, 0));
        bone.add("cubes", new JsonArray());
        this.bones.add(bone);
        if (outline.has("children")) {
            for (JsonElement child : outline.getAsJsonArray("children")) {
                this.addNode(child, bone);
            }
        }
        if (name.equals("hitbox") || name.equals("shadow") || name.startsWith("mount") || name.startsWith("nametag")) {
            bone.add("cubes", new JsonArray());
        }
    }

    private JsonObject cube(JsonObject source) {
        JsonArray from = source.getAsJsonArray("from");
        JsonArray to = source.getAsJsonArray("to");
        JsonObject cube = new JsonObject();
        cube.add("origin", vector(-to.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble()));
        cube.add("size", vector(to.get(0).getAsDouble() - from.get(0).getAsDouble(), to.get(1).getAsDouble() - from.get(1).getAsDouble(), to.get(2).getAsDouble() - from.get(2).getAsDouble()));
        cube.add("pivot", transformed(source.getAsJsonArray("origin"), -1, 1, 1, 0));
        cube.add("rotation", transformed(source.getAsJsonArray("rotation"), -1, -1, 1, 0));
        if (source.has("inflate")) {
            cube.add("inflate", source.get("inflate"));
        }
        JsonObject faces = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            if (!face.has("texture") || face.get("texture").isJsonNull()) {
                continue;
            }
            Texture texture = this.textures.get(face.get("texture").getAsInt());
            JsonArray uv = face.getAsJsonArray("uv");
            double u = uv.get(0).getAsDouble() * texture.sx();
            double v = uv.get(1).getAsDouble() * texture.sy() + texture.offset();
            double du = (uv.get(2).getAsDouble() - uv.get(0).getAsDouble()) * texture.sx();
            double dv = (uv.get(3).getAsDouble() - uv.get(1).getAsDouble()) * texture.sy();
            // Bedrock uses the opposite UV corner for horizontal faces. Face names stay unchanged.
            if (entry.getKey().equals("up") || entry.getKey().equals("down")) {
                u += du;
                v += dv;
                du = -du;
                dv = -dv;
            }
            JsonObject output = new JsonObject();
            output.add("uv", vector(u, v));
            output.add("uv_size", vector(du, dv));
            if (face.has("rotation")) {
                output.add("uv_rotation", face.get("rotation"));
            }
            faces.add(entry.getKey(), output);
        }
        cube.add("uv", faces);
        return cube;
    }

    private JsonObject animation(JsonObject source) {
        JsonObject animation = new JsonObject();
        String loop = source.has("loop") ? source.get("loop").getAsString() : "once";
        if (loop.equals("loop")) {
            animation.addProperty("loop", true);
        } else if (loop.equals("hold")) {
            animation.addProperty("loop", "hold_on_last_frame");
        }
        animation.add("animation_length", source.get("length"));
        JsonObject channels = new JsonObject();
        animation.add("bones", channels);
        if (!source.has("animators")) {
            return animation;
        }
        for (Map.Entry<String, JsonElement> entry : source.getAsJsonObject("animators").entrySet()) {
            String bone = this.boneIds.get(entry.getKey());
            if (bone == null) {
                continue;
            }
            JsonObject output = new JsonObject();
            JsonArray keyframes = entry.getValue().getAsJsonObject().getAsJsonArray("keyframes");
            if (keyframes == null) {
                continue;
            }
            for (JsonElement element : keyframes) {
                JsonObject frame = element.getAsJsonObject();
                String channel = frame.get("channel").getAsString();
                if (!List.of("rotation", "position", "scale").contains(channel)) {
                    continue;
                }
                JsonObject timeline = output.getAsJsonObject(channel);
                if (timeline == null) {
                    timeline = new JsonObject();
                    output.add(channel, timeline);
                }
                JsonArray points = frame.getAsJsonArray("data_points");
                JsonObject keyframe = new JsonObject();
                keyframe.add("post", animationPoint(points.get(points.size() - 1).getAsJsonObject(), channel));
                if (points.size() > 1) {
                    keyframe.add("pre", animationPoint(points.get(0).getAsJsonObject(), channel));
                }
                if (frame.has("interpolation") && frame.get("interpolation").getAsString().equals("catmullrom")) {
                    keyframe.addProperty("lerp_mode", "catmullrom");
                }
                timeline.add(frame.get("time").getAsString(), keyframe);
            }
            channels.add(bone, output);
        }
        return animation;
    }

    private static JsonArray animationPoint(JsonObject point, String channel) {
        JsonArray value = new JsonArray();
        for (String axis : List.of("x", "y", "z")) {
            int sign = channel.equals("scale") || axis.equals("z") || (axis.equals("y") && channel.equals("position")) ? 1 : -1;
            String expression = point.has(axis) ? point.get(axis).getAsString() : (channel.equals("scale") ? "1" : "0");
            if (expression.isBlank()) {
                expression = channel.equals("scale") ? "1" : "0";
            }
            try {
                value.add(Double.parseDouble(expression) * sign);
            } catch (NumberFormatException exception) {
                value.add(new JsonPrimitive(sign == 1 ? expression : "-(" + expression + ")"));
            }
        }
        return value;
    }

    private void save(Path root, String directory, String suffix, JsonObject value) throws IOException {
        Path output = root.resolve(directory + this.stem + suffix);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(value, output);
    }

    private static String bit(String name, int index) {
        return "math.mod(math.floor(q.property('craftengine:model_" + name + index / 19 + "') / " + (1 << (index % 19)) + "), 2)";
    }

    private static JsonArray transformed(JsonArray value, double x, double y, double z, double fallback) {
        return value == null ? vector(fallback, fallback, fallback) : vector(value.get(0).getAsDouble() * x, value.get(1).getAsDouble() * y, value.get(2).getAsDouble() * z);
    }

    private static JsonArray vector(double... values) {
        JsonArray result = new JsonArray(values.length);
        for (double value : values) {
            result.add(value);
        }
        return result;
    }

    private record Texture(BufferedImage image, int offset, double sx, double sy) {
    }
}
