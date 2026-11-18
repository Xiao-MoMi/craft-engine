package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions;
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
        attachment.animationNames.addProperty("hit", "animation.ce.spear.hit");
        attachment.animationNames.addProperty("hit_controller", "controller.animation.spear.hit");
        attachment.animate.add("held");
        attachment.animate.add("hit_controller");
        JsonArray preAnimation = BedrockItemRenderStrategy.useScripts(plan.item().useBehavior());
        preAnimation.add(BedrockItemAttachables.stageSelection(conditions, Function.identity()));
        attachment.write(root, preAnimation);
        ResourcePackUtils.writeBedrockJson(BedrockFormatVersions.ANIMATION, "animations", Templates.HIT_ANIMATIONS,
                root.resolve("animations/ce/shared/spear_hit.animation.json"));
    }

    private static JsonObject geometry(String identifier, String texture) {
        JsonObject geometry = Templates.GEOMETRY.deepCopy();
        geometry.getAsJsonObject("description").addProperty("identifier", identifier);
        geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("texture_meshes")
                .get(0).getAsJsonObject().addProperty("texture", texture);
        return geometry;
    }

    private static final class Templates {
        private static final JsonObject GEOMETRY = GsonHelper.readJsonFromResource("/bedrock/item/spear/flat.geo.json")
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        private static final JsonObject HIT_ANIMATIONS = GsonHelper.createObject("animation.ce.spear.hit",
                GsonHelper.readJsonFromResource("/bedrock/item/spear/flat.animation.json")
                        .getAsJsonObject().getAsJsonObject("animations").getAsJsonObject("animation.spear.hit"));
    }
}
