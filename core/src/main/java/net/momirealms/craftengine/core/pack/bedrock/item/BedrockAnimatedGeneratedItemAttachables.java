package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockAnimatedGeneratedItemAttachables {
    private static final String MESH_BONE = "ce_mesh_pose";
    private static final float MODEL_RADIUS = (float) Math.sqrt(8 * 8 + 8 * 8 + 0.5f * 0.5f);

    private BedrockAnimatedGeneratedItemAttachables() {
    }

    static boolean write(Key id, ItemBindingRule rule, JavaModelResolver models, JavaPackTextureSource textures, Path root) throws IOException {
        if (!rule.transformations().isEmpty() || !(rule.target() instanceof ItemRenderTarget.Model target)) {
            return false;
        }
        JavaModel model = models.resolve(target.model());
        if (model.kind() != JavaModel.GeometryKind.GENERATED) {
            return false;
        }
        List<JavaModel.Texture> layers = models.resolveLayers(model);
        if (layers.size() != 1) {
            return false;
        }
        TextureData data = textures.read(layers.getFirst(), TextureUsage.ITEM);
        TextureAnimation animation = data.animation();
        if (animation == null || !hasStableAlpha(animation)) {
            return false;
        }
        int color = target.tints().isEmpty() ? WHITE : textures.resolveTint(target.tints().getFirst());
        write(id, model, data, animation, color, root);
        return true;
    }

    private static boolean hasStableAlpha(TextureAnimation animation) {
        BufferedImage first = animation.firstFrame();
        int width = animation.width();
        int height = animation.height();
        int columns = animation.sheet().getWidth() / width;
        BitSet visited = new BitSet();
        visited.set(animation.frames().getFirst().index());
        // 原生网格只在加载时按首帧生成；alpha 不一致时保留原有的全帧边缘合并路径。
        for (TextureAnimation.Frame frame : animation.frames()) {
            if (visited.get(frame.index())) {
                continue;
            }
            visited.set(frame.index());
            int left = frame.index() % columns * width;
            int top = frame.index() / columns * height;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((first.getRGB(x, y) >>> 24) != (animation.sheet().getRGB(left + x, top + y) >>> 24)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void write(Key id, JavaModel model, TextureData data, TextureAnimation animation, int color, Path root) throws IOException {
        String resource = id.namespace() + '/' + id.value();
        String prefix = ResourcePackUtils.bedrockResourceIdentifier(id);
        String geometryId = "geometry." + prefix + ".hand";
        String controllerId = "controller.render." + prefix + ".hand";
        String animationPrefix = "animation." + prefix + '.';
        JsonObject textures = new JsonObject();
        textures.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        JsonArray frames = BedrockItemTextures.writeFrames(data, color, "default", "textures/" + resource + "/hand/layer_0", textures, root);
        textures.add("default", textures.get("default_frame_0"));

        JsonObject geometry = BedrockItemAttachables.displayGeometry(model, new JsonArray(), geometryId, MODEL_RADIUS);
        JsonObject description = geometry.getAsJsonObject("description");
        int width = animation.width();
        int height = animation.height();
        description.addProperty("texture_width", width);
        description.addProperty("texture_height", height);
        JsonArray bones = geometry.getAsJsonArray("bones");
        JsonObject parent = bones.get(bones.size() - 1).getAsJsonObject();
        parent.remove("cubes");
        JsonObject mesh = new JsonObject();
        mesh.addProperty("texture", "default");
        mesh.add("local_pivot", VectorUtils.toJson(width / 2f, 0.5f, height / 2f));
        mesh.add("position", VectorUtils.toJson(0, 8, 0));
        mesh.add("rotation", VectorUtils.toJson(0, 0, 0));
        mesh.add("scale", VectorUtils.toJson(16f / width, 1, 16f / height));
        mesh.addProperty("use_pixel_depth", true);
        JsonArray meshes = new JsonArray();
        meshes.add(mesh);
        JsonObject child = new JsonObject();
        child.addProperty("name", MESH_BONE);
        child.addProperty("parent", parent.get("name").getAsString());
        child.add("pivot", VectorUtils.toJson(0, 8, 0));
        child.add("texture_meshes", meshes);
        bones.add(child);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        writeFile("1.16.0", "minecraft:geometry", geometries, root.resolve("models/entity/" + resource + ".geo.json"));

        JsonObject animations = new JsonObject();
        JsonObject names = new JsonObject();
        JsonArray animate = new JsonArray();
        BedrockItemAttachables.displayAnimations(model, animationPrefix, -1, animations, names, animate);
        // texture_mesh 内部旋转只转顶点、不转法线；独立骨骼同时旋转二者，避免受光方向错误。
        JsonObject pose = new JsonObject();
        pose.add("rotation", VectorUtils.toJson(-90, 180, 0));
        JsonObject poseBones = new JsonObject();
        poseBones.add(MESH_BONE, pose);
        JsonObject poseAnimation = new JsonObject();
        poseAnimation.addProperty("loop", true);
        poseAnimation.add("bones", poseBones);
        animations.add(animationPrefix + "mesh_pose", poseAnimation);
        names.addProperty("mesh_pose", animationPrefix + "mesh_pose");
        animate.add("mesh_pose");
        writeFile("1.8.0", "animations", animations, root.resolve("animations/" + resource + ".animation.json"));

        JsonObject controller = new JsonObject();
        controller.addProperty("geometry", "Geometry.default");
        JsonObject frameArrays = new JsonObject();
        frameArrays.add("array.frames", frames);
        JsonObject arrays = new JsonObject();
        arrays.add("textures", frameArrays);
        controller.add("arrays", arrays);
        JsonArray selectedTextures = new JsonArray();
        selectedTextures.add("array.frames[math.floor(math.mod(query.life_time * 20, " + animation.duration() + ") / " + animation.sampleInterval() + ")]");
        selectedTextures.add("Texture.enchanted");
        controller.add("textures", selectedTextures);
        JsonObject material = new JsonObject();
        material.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        JsonArray selectedMaterials = new JsonArray();
        selectedMaterials.add(material);
        controller.add("materials", selectedMaterials);
        JsonObject controllers = new JsonObject();
        controllers.add(controllerId, controller);
        writeFile("1.10.0", "render_controllers", controllers, root.resolve("render_controllers/" + resource + ".render_controllers.json"));

        JsonObject attachment = new JsonObject();
        attachment.addProperty("identifier", id.asString());
        JsonObject materials = new JsonObject();
        materials.addProperty("default", "entity_alphatest");
        materials.addProperty("enchanted", "entity_alphatest_glint");
        attachment.add("materials", materials);
        attachment.add("textures", textures);
        JsonObject geometryNames = new JsonObject();
        geometryNames.addProperty("default", geometryId);
        attachment.add("geometry", geometryNames);
        attachment.add("animations", names);
        JsonObject scripts = new JsonObject();
        scripts.add("animate", animate);
        attachment.add("scripts", scripts);
        JsonObject activeController = new JsonObject();
        activeController.addProperty(controllerId, "context.item_slot == 'main_hand' || context.item_slot == 'off_hand'");
        JsonArray activeControllers = new JsonArray();
        activeControllers.add(activeController);
        attachment.add("render_controllers", activeControllers);
        JsonObject attachable = new JsonObject();
        attachable.add("description", attachment);
        writeFile("1.10.0", "minecraft:attachable", attachable, root.resolve("attachables/" + resource + ".attachable.json"));
    }

    private static void writeFile(String version, String key, JsonElement value, Path path) throws IOException {
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, version);
        file.add(key, value);
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(file, path);
    }
}
