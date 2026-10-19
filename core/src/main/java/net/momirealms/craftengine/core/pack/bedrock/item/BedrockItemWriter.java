package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeometryWriter;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockAnimatedTextures;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class BedrockItemWriter {
    private static final String HIDE_ANIMATION = "firstperson_head";
    private static final String HIDE_CONDITION = "context.is_first_person && context.item_slot == 'head'";

    private BedrockItemWriter() {
    }

    public static Output write(BedrockModelConverter.Result model, Key identifier, Path packRoot) throws IOException {
        return write(model, identifier, packRoot, List.of());
    }

    public static Output write(BedrockModelConverter.Result model, Key identifier, Path packRoot,
                               List<BedrockAnimatedTextures.Track> tracks) throws IOException {
        Resources resources = writeResources(model, identifier, packRoot, tracks, "");
        Path attachablePath = writeAttachable(identifier, identifier, model.geometry().identifier(), packRoot, tracks);
        return new Output(resources.geometry(), resources.texture(), resources.animations(), attachablePath);
    }

    static Resources writeResources(BedrockModelConverter.Result model, Key identifier, Path packRoot,
                                    List<BedrockAnimatedTextures.Track> tracks, String bonePrefix) throws IOException {
        String resource = identifier.namespace() + '/' + identifier.value();
        String texture = "textures/" + resource;
        String animationPrefix = "animation." + identifier.namespace() + "." + identifier.value() + ".";
        Map<BedrockItemDisplay.View, BedrockItemDisplay.Transform> transforms = BedrockItemDisplay.convert(model.source());
        Path geometryPath = packRoot.resolve("models/entity/" + resource + ".geo.json");
        Path texturePath = packRoot.resolve(texture + ".png");
        Path animationsPath = packRoot.resolve("animations/" + resource + ".animation.json");
        BedrockGeometry geometry = model.geometry();
        BedrockGeometry held = tracks.isEmpty() ? geometry : BedrockAnimatedItem.select(geometry, null, geometry.identifier(), geometry.textureWidth(), geometry.textureHeight());
        BedrockGeometry.Bounds bounds = BedrockItemDisplay.bounds(geometry, transforms);
        BedrockGeometryWriter.write(model, geometryPath, texturePath, BedrockItemDisplay.rig(held, bonePrefix), bounds);
        BedrockAnimatedItem.write(geometry, bounds, tracks, packRoot, bonePrefix);
        Files.createDirectories(animationsPath.getParent());
        GsonHelper.writeJsonFile(animations(transforms, animationPrefix, tracks, bonePrefix), animationsPath);
        return new Resources(geometryPath, texturePath, animationsPath);
    }

    public static Path writeAttachable(Key identifier, Key resources, String geometry, Path packRoot) throws IOException {
        return writeAttachable(identifier, resources, geometry, packRoot, List.of());
    }

    public static Path writeAttachable(Key identifier, Key resources, String geometry, Path packRoot,
                                       List<BedrockAnimatedTextures.Track> tracks) throws IOException {
        Path path = packRoot.resolve("attachables/" + identifier.namespace() + '/' + identifier.value() + ".attachable.json");
        String texture = "textures/" + resources.namespace() + '/' + resources.value();
        String animations = "animation." + resources.namespace() + "." + resources.value() + ".";
        Files.createDirectories(path.getParent());
        JsonObject attachable = attachable(identifier, geometry, texture, animations);
        BedrockAnimatedItem.bind(attachable.getAsJsonObject("minecraft:attachable").getAsJsonObject("description"), geometry, tracks, "");
        GsonHelper.writeJsonFile(attachable, path);
        return path;
    }

    private static JsonObject animations(Map<BedrockItemDisplay.View, BedrockItemDisplay.Transform> transforms, String prefix,
                                         List<BedrockAnimatedTextures.Track> tracks, String bonePrefix) {
        JsonObject animations = new JsonObject();
        for (BedrockItemDisplay.View view : BedrockItemDisplay.VIEWS) {
            BedrockItemDisplay.Transform transform = transforms.get(view);
            JsonObject slot = new JsonObject();
            slot.add("rotation", VectorUtils.toJson(view.pose.rotation()));
            slot.add("position", VectorUtils.toJson(view.pose.position()));
            slot.add("scale", VectorUtils.toJson(view.pose.scale()));
            JsonObject x = new JsonObject();
            x.add("position", VectorUtils.toJson(transform.translation()));
            x.add("rotation", axisRotation(0, transform.rotation().x()));
            JsonObject y = new JsonObject();
            y.add("rotation", axisRotation(1, transform.rotation().y()));
            JsonObject z = new JsonObject();
            z.add("rotation", axisRotation(2, transform.rotation().z()));
            z.add("scale", VectorUtils.toJson(transform.scale()));
            JsonObject bones = new JsonObject();
            addBoneAnimation(bones, bonePrefix, BedrockItemDisplay.SLOT_BONE, slot, tracks);
            addBoneAnimation(bones, bonePrefix, BedrockItemDisplay.X_BONE, x, tracks);
            addBoneAnimation(bones, bonePrefix, BedrockItemDisplay.Y_BONE, y, tracks);
            addBoneAnimation(bones, bonePrefix, BedrockItemDisplay.Z_BONE, z, tracks);
            animations.add(prefix + view.animation, animation(bones));
        }
        JsonObject hidden = new JsonObject();
        hidden.addProperty("scale", 0);
        JsonObject bones = new JsonObject();
        addBoneAnimation(bones, bonePrefix, BedrockItemDisplay.SLOT_BONE, hidden, tracks);
        animations.add(prefix + HIDE_ANIMATION, animation(bones));
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", animations);
        return root;
    }

    static void addBoneAnimation(JsonObject bones, String prefix, String name, JsonObject transform, List<BedrockAnimatedTextures.Track> tracks) {
        bones.add(prefix + name, transform);
        // Separate rendered geometries need distinct bone names, but must receive identical display and hiding poses.
        for (BedrockAnimatedTextures.Track track : tracks) {
            bones.add(prefix + track.material() + '_' + name, transform);
        }
    }

    private static JsonObject animation(JsonObject bones) {
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
    }

    private static JsonArray axisRotation(int axis, float angle) {
        JsonArray rotation = new JsonArray();
        for (int index = 0; index < 3; index++) {
            rotation.add(index == axis ? angle : 0);
        }
        return rotation;
    }

    static JsonObject attachable(Key identifier, String geometry, String texture, String prefix) {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", BedrockItemSource.bedrockIdentifier(identifier));
        JsonObject materials = new JsonObject();
        materials.addProperty("default", "entity_alphatest_one_sided");
        materials.addProperty("enchanted", "entity_alphatest_glint");
        description.add("materials", materials);
        JsonObject textures = new JsonObject();
        textures.addProperty("default", texture);
        textures.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        description.add("textures", textures);
        JsonObject geometries = new JsonObject();
        geometries.addProperty("default", geometry);
        description.add("geometry", geometries);
        JsonObject animations = new JsonObject();
        JsonArray animate = new JsonArray();
        for (BedrockItemDisplay.View view : BedrockItemDisplay.VIEWS) {
            animations.addProperty(view.animation, prefix + view.animation);
            JsonObject condition = new JsonObject();
            condition.addProperty(view.animation, view.condition);
            animate.add(condition);
        }
        animations.addProperty(HIDE_ANIMATION, prefix + HIDE_ANIMATION);
        JsonObject hide = new JsonObject();
        hide.addProperty(HIDE_ANIMATION, HIDE_CONDITION);
        animate.add(hide);
        description.add("animations", animations);
        JsonObject scripts = new JsonObject();
        scripts.add("animate", animate);
        description.add("scripts", scripts);
        JsonArray controllers = new JsonArray();
        controllers.add("controller.render.item_default");
        description.add("render_controllers", controllers);
        JsonObject attachable = new JsonObject();
        attachable.add("description", description);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.10.0");
        root.add("minecraft:attachable", attachable);
        return root;
    }

    public record Output(Path geometry, Path texture, Path animations, Path attachable) {
    }

    record Resources(Path geometry, Path texture, Path animations) {
    }
}
