package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public final class BedrockSpear2DAttachables {

    private BedrockSpear2DAttachables() {
    }

    static void write(BedrockItemPresentation plan, List<String> conditions, List<BufferedImage> stages, Path root) throws IOException {
        String resource = plan.id().namespace() + '/' + plan.id().value();
        BedrockItemAttachableBuilder attachment = new BedrockItemAttachableBuilder(plan.id());
        JsonArray geometryFrames = new JsonArray();
        JsonArray textureFrames = new JsonArray();
        for (int i = 0; i < stages.size(); i++) {
            String alias = "stage_" + i;
            String identifier = "geometry." + attachment.prefix + '.' + alias;
            String texture = "textures/" + resource + "/hand/stage_" + i;
            ImageUtils.writePng(stages.get(i), root.resolve(texture + ".png"));
            attachment.addGeometry(alias, identifier, texture, geometry(identifier, alias));
            geometryFrames.add("Geometry." + alias);
            textureFrames.add("Texture." + alias);
        }
        attachment.writeStageController(root, geometryFrames, textureFrames);
        attachment.animationNames.addProperty("held_first_person", "animation.spear.held_first_person");
        attachment.animationNames.addProperty("held_third_person", "animation.spear.held_third_person");
        attachment.animationNames.addProperty("held", "controller.animation.spear.held");
        attachment.animationNames.addProperty("hit", "animation.craftengine.spear.hit");
        attachment.animationNames.addProperty("hit_controller", "controller.animation.spear.hit");
        attachment.animate.add("held");
        attachment.animate.add("hit_controller");
        JsonArray preAnimation = BedrockItemRenderStrategy.useScripts(plan.item().useBehavior());
        preAnimation.add(BedrockItemAttachables.stageSelection(conditions, Function.identity()));
        attachment.write(root, preAnimation);
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/item/spear/flat.animation.json").getAsJsonObject();
        JsonObject nativeAnimations = animationFile.getAsJsonObject("animations");
        JsonObject hit = nativeAnimations.remove("animation.spear.hit").getAsJsonObject();
        JsonObject hitAnimations = new JsonObject();
        hitAnimations.add("animation.craftengine.spear.hit", hit);
        animationFile.add("animations", hitAnimations);
        ResourcePackUtils.writeJson(animationFile, root.resolve("animations/craftengine/shared/spear_hit.animation.json"));
    }

    private static JsonObject geometry(String identifier, String texture) {
        JsonObject geometry = GsonHelper.readJsonFromResource("/bedrock/item/spear/flat.geo.json")
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        geometry.getAsJsonObject("description").addProperty("identifier", identifier);
        geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("texture_meshes")
                .get(0).getAsJsonObject().addProperty("texture", texture);
        return geometry;
    }
}
