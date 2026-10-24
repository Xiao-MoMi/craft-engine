package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

public final class Bedrock2DCrossbowAttachables {

    private Bedrock2DCrossbowAttachables() {
    }

    static void write(BedrockCrossbowStates.State state, List<BedrockItemAttachables.Stage> stages,
                      ItemBehaviorDescription.Use use, Path root) throws IOException {
        String resource = state.id().namespace() + '/' + state.id().value();
        String prefix = ResourcePackUtils.bedrockResourceIdentifier(state.id());
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
            textures.addProperty(alias, stages.get(i).texture());
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
        description.addProperty("identifier", state.id().asString());
        JsonObject materials = new JsonObject();
        materials.addProperty("default", "entity_alphatest");
        materials.addProperty("enchanted", "entity_alphatest_glint");
        description.add("materials", materials);
        description.add("textures", textures);
        description.add("geometry", names);
        JsonObject animations = new JsonObject();
        animations.addProperty("wield", "animation.crossbow.wield");
        animations.addProperty("pull", "animation.crossbow.wield_first_person_pull");
        description.add("animations", animations);
        JsonArray animate = new JsonArray();
        animate.add("wield");
        JsonObject pulling = new JsonObject();
        pulling.addProperty("pull", "context.is_first_person && variable.ce_using_item");
        animate.add(pulling);
        JsonArray preAnimation = BedrockCrossbowStates.scripts(state, use);
        preAnimation.add("variable.charge_amount = variable.ce_charge_amount;");
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
    }

    private static JsonObject geometry(String identifier, String texture) {
        // 原版弩各阶段共用这一纹理网格姿态，只有贴图不同。
        JsonObject mesh = new JsonObject();
        mesh.add("position", VectorUtils.toJson(0, -5, -3));
        mesh.add("rotation", VectorUtils.toJson(90, 0, -49));
        mesh.addProperty("texture", texture);
        JsonArray meshes = new JsonArray();
        meshes.add(mesh);
        JsonObject bone = new JsonObject();
        bone.addProperty("name", "rightitem");
        bone.addProperty("binding", "query.item_slot_to_bone_name(context.item_slot)");
        bone.add("texture_meshes", meshes);
        JsonArray bones = new JsonArray();
        bones.add(bone);
        JsonObject description = new JsonObject();
        description.addProperty("identifier", identifier);
        description.addProperty("texture_width", 16);
        description.addProperty("texture_height", 16);
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        return geometry;
    }

    private static void writeJson(JsonObject json, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(json, path);
    }
}
