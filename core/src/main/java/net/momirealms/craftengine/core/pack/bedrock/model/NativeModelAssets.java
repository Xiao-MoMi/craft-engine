package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.BedrockEntityDye;
import net.momirealms.craftengine.core.pack.bedrock.item.display.DisplayDyeMasks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTextures;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

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
import java.util.function.Function;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.*;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

public final class NativeModelAssets {
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

    public static void write(List<NativeModelSource> sources, Path root, Path modelData, Function<Key, BedrockEntityDye> dyes) throws IOException {
        if (sources.isEmpty()) {
            return;
        }
        JsonObject models = new JsonObject();
        for (NativeModelSource source : sources) {
            try {
                String material = switch (source.id().namespace()) {
                    case "bettermodel", "modelengine" -> "entity_alphatest_one_sided";
                    default -> "entity_alphatest";
                };
                models.add(source.id().asString(), new NativeModelAssets(source).write(root, dyes.apply(source.id()), material));
            } catch (RuntimeException exception) {
                throw new IOException("Cannot convert native model " + source.id() + " from " + source.blueprint(), exception);
            }
        }
        Files.createDirectories(modelData.getParent());
        GsonHelper.writeJsonFile(models, modelData);
    }

    private JsonObject write(Path root, BedrockEntityDye dye, String material) throws IOException {
        // 白色直接使用基础贴图，不参与原生模型的预染色匹配和属性注册。
        if (dye.prebakedColors().contains(0xffffff)) {
            dye = new BedrockEntityDye(dye.maxLayers(), dye.prebakedColors().stream().filter(rgb -> rgb != 0xffffff).toList());
        }
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

        // Geyser 用清单中的原始名称，把插件传来的骨骼和动画状态映射到资源包里的编号。
        JsonObject manifest = new JsonObject();
        manifest.addProperty("dye", dye.maxLayers() > 0 || !dye.prebakedColors().isEmpty());
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
                // 每个实体属性打包 9 个动画，每个动画占 2 位，依次表示停止、单次播放、循环和停在末帧
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
        if (!controllers.isEmpty()) {
            this.save(root, "animation_controllers/ce/native/", ".json", ANIMATION_CONTROLLER, createObject("animation_controllers", controllers));
        }

        // 每个属性记录 19 根骨骼的隐藏位。这里的遍历顺序要与清单中的骨骼编号一致。
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
        // 预染色按 RGB 精确匹配；其余颜色在允许动态染色时使用蒙版，否则回到基础贴图。
        String base = dye.baseCondition("ce:model_dye", true);
        if (base.isEmpty()) {
            activeControllers.add(renderId);
        } else {
            activeControllers.add(createObject(renderId, new JsonPrimitive(base)));
        }
        TextureData atlasTexture = new TextureData(this.atlas, null);
        for (int rgb : dye.prebakedColors()) {
            String alias = "baked_" + Integer.toHexString(rgb);
            JsonArray frames = BedrockItemTextures.writeFrames(atlasTexture, rgb, alias, texture + '/' + alias, entityTextures, root);
            String bakedId = renderId + "." + alias;
            renderControllers.add(bakedId, renderController(frames, partVisibility));
            activeControllers.add(createObject(bakedId, new JsonPrimitive(BedrockEntityDye.matches("ce:model_dye", rgb))));
        }
        if (dye.maxLayers() > 0) {
            this.addDynamicDye(root, texture, atlasTexture, dye, entityTextures, renderId, renderControllers, activeControllers, partVisibility);
        }
        this.save(root, "render_controllers/ce/native/", ".json", RENDER_CONTROLLER, createObject("render_controllers", renderControllers));
        JsonObject entity = createObject("minecraft:client_entity", createObject("description", entityDescription));
        this.save(root, "entity/ce/native/", ".json", CLIENT_ENTITY, entity);
        return manifest;
    }

    private void addDynamicDye(Path root,
                               String texture,
                               TextureData atlasTexture,
                               BedrockEntityDye dye,
                               JsonObject entityTextures,
                               String renderId,
                               JsonObject renderControllers,
                               JsonArray activeControllers,
                               JsonArray partVisibility) throws IOException {
        BitSet coverage = new BitSet(this.atlas.getWidth() * this.atlas.getHeight());
        coverage.set(0, this.atlas.getWidth() * this.atlas.getHeight());
        DisplayDyeMasks masks = new DisplayDyeMasks(atlasTexture, coverage);
        Int2ObjectMap<JsonArray> layers = masks.write("dye", texture + '/', entityTextures, root, dye.maxLayers());
        String condition = dye.dynamicCondition("ce:model_dye", true);
        for (Int2ObjectMap.Entry<JsonArray> layer : layers.int2ObjectEntrySet()) {
            JsonObject tinted = renderController(layer.getValue(), partVisibility);
            int shade = layer.getIntKey();
            JsonObject overlay = new JsonObject();
            overlay.addProperty("r", "q.property('ce:model_dye_r') / 255 * " + (shade >> 16) + " / 255 * 0.72");
            overlay.addProperty("g", "math.floor(q.property('ce:model_dye_gb') / 256) / 255 * " + (shade >> 8 & 255) + " / 255 * 0.72");
            overlay.addProperty("b", "math.mod(q.property('ce:model_dye_gb'), 256) / 255 * " + (shade & 255) + " / 255 * 0.72");
            overlay.addProperty("a", 1);
            tinted.add("overlay_color", overlay);
            String tintedId = renderId + ".d" + Integer.toHexString(shade);
            renderControllers.add(tintedId, tinted);
            activeControllers.add(createObject(tintedId, new JsonPrimitive(condition)));
        }
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
            // 原图纵向拼入图集。保留每张图的纵向偏移和 UV 到像素的比例，供转换方块面时使用。
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
            // 基岩版从 UV 矩形的对角读取顶面和底面，因此把起点移到对角，并反转两个方向的步长。
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
                // 同一关键帧有多个采样点时，首尾分别对应进入和离开该帧的值，以保留帧上的突变
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
