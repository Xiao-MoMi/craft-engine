package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.item.display.DisplayDyeMasks;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.GsonHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.*;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

public final class NativeModelAssets {
    public static final String MANIFEST = BedrockPackLayout.NATIVE_MODELS;
    private static final String DEFAULT_COLOR = "q.property('ce:model_dye_r') < 0 || (q.property('ce:model_dye_r') == 255 && q.property('ce:model_dye_gb') == 65535)";
    private static final String DYE_RED = "q.property('ce:model_dye_r') < 0 ? 1 : q.property('ce:model_dye_r') / 255";
    private static final String DYE_GREEN = "q.property('ce:model_dye_r') < 0 ? 1 : math.floor(q.property('ce:model_dye_gb') / 256) / 255";
    private static final String DYE_BLUE = "q.property('ce:model_dye_r') < 0 ? 1 : math.mod(q.property('ce:model_dye_gb'), 256) / 255";
    private static final List<String> NON_RENDERABLE_TYPES = List.of("bounding_box", "locator", "null_object", "camera");
    private static final List<String> ANIMATION_CHANNELS = List.of("rotation", "position", "scale");
    private static final List<String> AXES = List.of("x", "y", "z");
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
        String texture = BedrockPackLayout.NATIVE_MODEL_TEXTURES + '/' + this.stem;
        int[] size = this.writeTexture(root.resolve(texture + ".png"));
        for (JsonElement element : this.blueprint.getAsJsonArray("elements")) {
            JsonObject cube = element.getAsJsonObject();
            if (cube.has("type") && NON_RENDERABLE_TYPES.contains(cube.get("type").getAsString())) {
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
        String geometryId = "geometry.ce_native_" + this.stem;
        JsonObject description = new JsonObject();
        description.addProperty("identifier", geometryId);
        description.addProperty("texture_width", size[0]);
        description.addProperty("texture_height", size[1]);
        description.addProperty("visible_bounds_width", 64);
        description.addProperty("visible_bounds_height", 64);
        description.add("visible_bounds_offset", vector(0, 0, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", this.bones);
        this.save(root, "models/entity/ce/native/", ".geo.json", GEOMETRY, createObject("minecraft:geometry", single(geometry)));

        JsonObject manifest = new JsonObject();
        manifest.addProperty("identifier", "ce:native_" + this.stem);
        JsonObject names = new JsonObject();
        this.boneNames.forEach(names::addProperty);
        manifest.add("bones", names);
        JsonArray animationNames = new JsonArray();
        manifest.add("animations", animationNames);
        JsonObject animations = new JsonObject();
        JsonObject controllers = new JsonObject();
        JsonObject aliases = new JsonObject();
        JsonArray animate = new JsonArray();
        String animationPrefix = "animation.ce_native_" + this.stem + '.';
        if (this.blueprint.has("animations")) {
            for (JsonElement element : this.blueprint.getAsJsonArray("animations")) {
                JsonObject animation = element.getAsJsonObject();
                int index = animationNames.size();
                animationNames.add(animation.get("name").getAsString());
                String alias = "a" + index;
                String id = animationPrefix + alias;
                JsonObject converted = this.animation(animation);
                String controllerId = "controller.animation.ce_native_" + this.stem + "." + alias;
                JsonArray startTransitions = new JsonArray(3);
                JsonObject states = createObject("stop", createObject("transitions", startTransitions));
                JsonObject controller = new JsonObject();
                controller.addProperty("initial_state", "stop");
                controller.add("states", states);
                String property = "math.mod(math.floor(q.property('ce:model_anim" + index / 9 + "') / " + (1 << (index % 9 * 2)) + "), 4)";
                for (int mode = 1; mode <= 3; mode++) {
                    String variant = alias + "_" + mode;
                    // 各播放模式只替换顶层的 loop，共用已转换的骨骼关键帧
                    JsonObject clip = new JsonObject();
                    clip.asMap().putAll(converted.asMap());
                    if (mode == 3) {
                        clip.addProperty("loop", "hold_on_last_frame");
                    } else {
                        clip.addProperty("loop", mode == 2);
                    }
                    String clipId = id + "_" + mode;
                    animations.add(clipId, clip);
                    aliases.addProperty(variant, clipId);
                    JsonObject transition = new JsonObject();
                    transition.addProperty(variant, property + " == " + mode);
                    startTransitions.add(transition);
                    JsonObject play = createObject("animations", single(new JsonPrimitive(variant)));
                    JsonObject stop = new JsonObject();
                    stop.addProperty("stop", property + " != " + mode);
                    play.add("transitions", single(stop));
                    states.add(variant, play);
                }
                controllers.add(controllerId, controller);
                aliases.addProperty("c" + index, controllerId);
                animate.add("c" + index);
            }
        }
        String scaleId = animationPrefix + "scale";
        animations.add(scaleId, Templates.SCALE_ANIMATION);
        aliases.addProperty("scale", scaleId);
        animate.add("scale");
        this.save(root, "animations/ce/native/", ".json", ANIMATION, createObject("animations", animations));
        this.save(root, "animation_controllers/ce/native/", ".json", ANIMATION_CONTROLLER, createObject("animation_controllers", controllers));

        JsonArray partVisibility = new JsonArray(this.boneNames.size());
        int boneIndex = 0;
        for (String name : this.boneNames.values()) {
            JsonObject visible = new JsonObject();
            visible.addProperty(name, bit("bone", boneIndex++) + " == 0");
            partVisibility.add(visible);
        }
        String renderId = "controller.render.ce_native_" + this.stem;
        JsonObject renderControllers = createObject(renderId, renderController(Templates.DEFAULT_TEXTURES, partVisibility));
        JsonObject entityTextures = createObject("default", new JsonPrimitive(texture));
        JsonArray activeControllers = new JsonArray();
        JsonObject entityDescription = new JsonObject();
        entityDescription.add("materials", createObject("default", new JsonPrimitive(material)));
        entityDescription.add("textures", entityTextures);
        entityDescription.add("geometry", createObject("default", new JsonPrimitive(geometryId)));
        entityDescription.add("scripts", createObject("animate", animate));
        entityDescription.add("render_controllers", activeControllers);
        entityDescription.add("identifier", manifest.get("identifier"));
        entityDescription.add("animations", aliases);
        activeControllers.add(createObject(renderId, Templates.DEFAULT_COLOR_CONDITION));
        BitSet coverage = new BitSet(this.atlas.getWidth() * this.atlas.getHeight());
        coverage.set(0, this.atlas.getWidth() * this.atlas.getHeight());
        DisplayDyeMasks masks = new DisplayDyeMasks(new TextureData(this.atlas, null), coverage);
        Int2ObjectMap<JsonArray> layers = masks.write("dye", texture + '/', entityTextures, root, maxLayers);
        for (Int2ObjectMap.Entry<JsonArray> layer : layers.int2ObjectEntrySet()) {
            JsonObject tinted = renderController(layer.getValue(), partVisibility);
            int shade = layer.getIntKey();
            JsonObject overlay = new JsonObject();
            overlay.addProperty("r", "(" + DYE_RED + ") * " + (shade >> 16) + " / 255 * 0.72");
            overlay.addProperty("g", "(" + DYE_GREEN + ") * " + (shade >> 8 & 255) + " / 255 * 0.72");
            overlay.addProperty("b", "(" + DYE_BLUE + ") * " + (shade & 255) + " / 255 * 0.72");
            overlay.addProperty("a", 1);
            tinted.add("overlay_color", overlay);
            String tintedId = renderId + ".d" + Integer.toHexString(shade);
            renderControllers.add(tintedId, tinted);
            activeControllers.add(createObject(tintedId, Templates.TINTED_COLOR_CONDITION));
        }
        this.save(root, "render_controllers/ce/native/", ".json", RENDER_CONTROLLER, createObject("render_controllers", renderControllers));
        JsonObject entity = createObject("minecraft:client_entity", createObject("description", entityDescription));
        this.save(root, "entity/ce/native/", ".entity.json", CLIENT_ENTITY, entity);
        return manifest;
    }

    private int[] writeTexture(Path output) throws IOException {
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
                if (!ANIMATION_CHANNELS.contains(channel)) {
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
        JsonArray value = new JsonArray(AXES.size());
        for (String axis : AXES) {
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

    private static JsonArray single(JsonElement element) {
        JsonArray array = new JsonArray(1);
        array.add(element);
        return array;
    }

    private static JsonObject renderController(JsonArray textures, JsonArray partVisibility) {
        JsonObject render = new JsonObject();
        render.addProperty("geometry", "Geometry.default");
        render.add("materials", Templates.DEFAULT_MATERIALS);
        render.add("textures", textures);
        render.add("part_visibility", partVisibility);
        return render;
    }

    // 首个原生模型写出时初始化，只读用于序列化，保护步骤会重新读取写出的 JSON
    private static final class Templates {
        private static final JsonObject SCALE_ANIMATION = JsonParser.parseString("""
                {
                  "loop": true,
                  "bones": {
                    "root": {
                      "scale": ["q.property('ce:model_sx')", "q.property('ce:model_sy')", "q.property('ce:model_sz')"]
                    }
                  }
                }
                """).getAsJsonObject();
        private static final JsonArray DEFAULT_MATERIALS = single(createObject("*", new JsonPrimitive("Material.default")));
        private static final JsonArray DEFAULT_TEXTURES = single(new JsonPrimitive("Texture.default"));
        private static final JsonPrimitive DEFAULT_COLOR_CONDITION = new JsonPrimitive(DEFAULT_COLOR);
        private static final JsonPrimitive TINTED_COLOR_CONDITION = new JsonPrimitive("!(" + DEFAULT_COLOR + ")");
    }

    private void save(Path root, String directory, String suffix, String formatVersion, JsonObject value) throws IOException {
        value.addProperty(FORMAT_VERSION, formatVersion);
        Path output = root.resolve(directory + this.stem + suffix);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(value, output);
    }

    private static String bit(String name, int index) {
        return "math.mod(math.floor(q.property('ce:model_" + name + index / 19 + "') / " + (1 << (index % 19)) + "), 2)";
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
