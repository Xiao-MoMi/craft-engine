package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

public final class Bedrock2DSpearAttachables {

    private Bedrock2DSpearAttachables() {
    }

    static void write(ItemRegistrationPlan.Entry entry, BedrockSpearModels models, List<BufferedImage> stages, Path root) throws IOException {
        String resource = entry.bedrockId().namespace() + '/' + entry.bedrockId().value();
        String prefix = ResourcePackUtils.bedrockResourceIdentifier(entry.bedrockId());
        JsonObject textures = new JsonObject();
        textures.addProperty("enchanted", "textures/misc/enchanted_item_glint");
        JsonObject names = new JsonObject();
        JsonArray geometries = new JsonArray();
        JsonArray geometryFrames = new JsonArray();
        JsonArray textureFrames = new JsonArray();
        for (int i = 0; i < stages.size(); i++) {
            String alias = "stage_" + i;
            String identifier = "geometry." + prefix + '.' + alias;
            names.addProperty(alias, identifier);
            String texture = "textures/" + resource + "/hand/stage_" + i;
            ImageUtils.writePng(stages.get(i), root.resolve(texture + ".png"));
            textures.addProperty(alias, texture);
            geometryFrames.add("Geometry." + alias);
            textureFrames.add("Texture." + alias);
            geometries.add(geometry(identifier, alias));
        }
        JsonObject geometryFile = new JsonObject();
        geometryFile.addProperty(FORMAT_VERSION, "1.16.0");
        geometryFile.add("minecraft:geometry", geometries);
        writeJson(geometryFile, root.resolve("models/entity/" + resource + ".geo.json"));
        String controllerId = "controller.render." + prefix;
        BedrockItemAttachables.writeController(controllerId, geometryFrames, textureFrames,
                root.resolve("render_controllers/" + resource + ".render_controllers.json"));
        JsonObject description = new JsonObject();
        description.addProperty("identifier", entry.bedrockId().asString());
        JsonObject materials = new JsonObject();
        materials.addProperty("default", "entity_alphatest");
        materials.addProperty("enchanted", "entity_alphatest_glint");
        description.add("materials", materials);
        description.add("textures", textures);
        description.add("geometry", names);
        JsonObject animations = new JsonObject();
        animations.addProperty("held_first_person", "animation.spear.held_first_person");
        animations.addProperty("held_third_person", "animation.spear.held_third_person");
        animations.addProperty("held", "controller.animation.spear.held");
        animations.addProperty("hit", "animation.craftengine.spear.hit");
        animations.addProperty("hit_controller", "controller.animation.spear.hit");
        description.add("animations", animations);
        JsonArray animate = new JsonArray();
        animate.add("held");
        animate.add("hit_controller");
        JsonArray preAnimation = models.scripts(entry.source().clientBehavior());
        JsonObject scripts = new JsonObject();
        scripts.add("pre_animation", preAnimation);
        scripts.add("animate", animate);
        description.add("scripts", scripts);
        JsonObject active = new JsonObject();
        active.addProperty(controllerId, "(context.item_slot == 'main_hand' || context.item_slot == 'off_hand') && variable.ce_stage >= 0");
        JsonArray controllers = new JsonArray();
        controllers.add(active);
        description.add("render_controllers", controllers);
        JsonObject attachable = new JsonObject();
        attachable.add("description", description);
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, "1.10.0");
        file.add("minecraft:attachable", attachable);
        writeJson(file, root.resolve("attachables/" + resource + ".attachable.json"));
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/spear/spear.animation.json").getAsJsonObject();
        JsonObject nativeAnimations = animationFile.getAsJsonObject("animations");
        JsonObject hit = nativeAnimations.remove("animation.spear.hit").getAsJsonObject();
        JsonObject hitAnimations = new JsonObject();
        hitAnimations.add("animation.craftengine.spear.hit", hit);
        animationFile.add("animations", hitAnimations);
        writeJson(animationFile, root.resolve("animations/craftengine/shared/spear_hit.animation.json"));
    }

    private static JsonObject geometry(String identifier, String texture) {
        JsonObject geometry = GsonHelper.readJsonFromResource("/bedrock/spear/spear.geo.json")
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        geometry.getAsJsonObject("description").addProperty("identifier", identifier);
        geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("texture_meshes")
                .get(0).getAsJsonObject().addProperty("texture", texture);
        return geometry;
    }

    private static void writeJson(JsonObject json, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(json, path);
    }
}
