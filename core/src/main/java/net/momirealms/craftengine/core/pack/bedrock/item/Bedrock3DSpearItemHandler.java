package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Bedrock3DSpearItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public Bedrock3DSpearItemHandler(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        BedrockHeldItemModels selected = BedrockHeldItemModels.collect(entry);
        if (selected == null) {
            return null;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>();
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (int i = 0; i < selected.hands().size(); i++) {
                ItemBindingRule rule = selected.hands().get(i);
                stages.add(geometry.createStage(rule, selected.condition(i)));
            }
            display = context.itemDisplayBlocks().write(entry.bedrockId(), List.of(selected.inventory()), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        List<String> poseScripts = poseScripts(entry);
        Bedrock3DSpearAttachables.write(entry, selected, stages, geometry.materials(), poseScripts, context.root());
        context.playerAnimations().registerSpearUsingJavaPose(entry.bedrockId(), poseScripts);
        return List.of(GeneratedItemVariant.block(entry.bedrockId(), selected.registration(), display, true));
    }

    private static List<String> poseScripts(ItemRegistrationPlan.Entry entry) {
        List<String> scripts = new ArrayList<>(7);
        ItemBehaviorDescription behavior = entry.clientBehavior();
        JsonElement component = entry.properties().components().get(DataComponentKeys.KINETIC_WEAPON);
        JsonObject kinetic = component == null || component.isJsonNull() ? null : component.getAsJsonObject();
        scripts.add("variable.ce_spear_delay = " + (kinetic == null || !kinetic.has("delay_ticks") ? 0 : kinetic.get("delay_ticks").getAsInt()) + ";");
        for (String phase : List.of("dismount", "knockback", "damage")) {
            int duration = 0;
            if (kinetic != null && kinetic.has(phase + "_conditions")) {
                duration = kinetic.getAsJsonObject(phase + "_conditions").get("max_duration_ticks").getAsInt();
            }
            scripts.add("variable.ce_spear_" + phase + " = " + duration + ";");
        }
        scripts.add("variable.ce_spear_forward = " + (kinetic == null || !kinetic.has("forward_movement") ? 0 : kinetic.get("forward_movement").getAsFloat()) + ";");
        scripts.add("variable.ce_spear_use = " + (kinetic != null && behavior.use().animation() == ItemBehaviorDescription.UseAnimation.SPEAR ? "variable.ce_using_item" : "0") + ";");
        scripts.add("variable.ce_spear_attack = " + (behavior.attack().animation() == ItemBehaviorDescription.SwingAnimation.STAB ? "variable.attack_time > 0" : "0") + ";");
        return scripts;
    }
}
