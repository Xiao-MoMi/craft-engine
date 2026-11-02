package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class BedrockTridentItemHandler implements BedrockItemHandler {
    private final JavaModelResources resources;
    private final BedrockItemIconRenderer renderer;

    BedrockTridentItemHandler(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        BedrockHeldItemModels models = BedrockHeldItemModels.collect(entry);
        if (models == null || !models.inventory().transformations().isEmpty()) {
            return null;
        }
        BufferedImage icon = this.renderer.render((ItemRenderTarget.Model) models.inventory().target());
        if (icon == null) {
            return null;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.resources.models(), this.resources.textures());
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(models.hands().size());
        try {
            for (int i = 0; i < models.hands().size(); i++) {
                stages.add(geometry.createStage(models.hands().get(i), models.condition(i)));
            }
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        // 背包图标和手持 attachable 共用一个物品 ID，蓄力时由客户端切换手持姿态。
        Key id = entry.bedrockId();
        String texture = "textures/" + id.namespace() + '/' + id.value() + "/icon";
        ImageUtils.writePng(icon, context.root().resolve(texture + ".png"));
        ItemBehaviorDescription behavior = entry.clientBehavior();
        JsonArray scripts = models.scripts(behavior);
        JsonArray bones = new JsonArray();
        JsonObject animations = new JsonObject();
        JsonArray animate = new JsonArray();
        if (behavior.use().animation() == ItemBehaviorDescription.UseAnimation.TRIDENT) {
            // Java 的第一人称举戟动作先于模型 display 变换，在 ce_slot 与 display 骨骼之间应用。
            JsonObject charge = GsonHelper.readJsonFromResource("/bedrock/item/trident/charge.pose.json").getAsJsonObject();
            scripts.addAll(charge.getAsJsonArray("pre_animation"));
            bones = charge.getAsJsonArray("bones");
            animations = charge.getAsJsonObject("animations");
            animate = charge.getAsJsonArray("animate");
        }
        Bedrock3DItemAttachables.write(id, stages, geometry.materials(), scripts, bones, animations, animate, context.root());
        return List.of(GeneratedItemVariant.icon(id, models.registration(), texture, true));
    }
}
