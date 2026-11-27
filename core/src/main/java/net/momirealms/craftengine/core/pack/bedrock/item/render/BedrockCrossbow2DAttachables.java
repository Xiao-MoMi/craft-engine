package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class BedrockCrossbow2DAttachables {

    private BedrockCrossbow2DAttachables() {
    }

    static void write(BedrockCrossbowStates.State state,
                      List<BedrockItemAttachables.Stage> stages,
                      ItemBehaviorDescription.Use use,
                      Path root) throws IOException {
        BedrockItemAttachableBuilder attachment = new BedrockItemAttachableBuilder(state.id());
        JsonArray geometryFrames = new JsonArray();
        JsonArray textureFrames = new JsonArray();
        for (int i = 0; i < stages.size(); i++) {
            String alias = "stage_" + i;
            String identifier = "geometry." + attachment.prefix + '.' + alias;
            attachment.addGeometry(alias, identifier, stages.get(i).texture(), geometry(identifier, alias));
            geometryFrames.add("Geometry." + alias);
            textureFrames.add("Texture." + alias);
        }
        attachment.writeStageController(root, geometryFrames, textureFrames);
        attachment.animationNames.addProperty("wield", "animation.crossbow.wield");
        attachment.animationNames.addProperty("pull", "animation.crossbow.wield_first_person_pull");
        attachment.animate.add("wield");
        JsonObject pulling = new JsonObject();
        pulling.addProperty("pull", "context.is_first_person && variable.ce_using_item");
        attachment.animate.add(pulling);
        JsonArray preAnimation = BedrockCrossbowStates.scripts(state, use);
        preAnimation.add("variable.charge_amount = variable.ce_charge_amount;");
        attachment.write(root, preAnimation);
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
}
