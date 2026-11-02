package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.ImageUtils;
import org.joml.Matrix4f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Bedrock2DCrossbowItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final BedrockItemIconRenderer renderer;

    public Bedrock2DCrossbowItemHandler(JavaModelResolver models, BedrockItemIconRenderer renderer) {
        this.models = models;
        this.renderer = renderer;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<BedrockCrossbowStates.State> states = BedrockCrossbowStates.collect(entry);
        ItemBehaviorDescription.Ticks chargeDuration = entry.chargeDuration();
        if (states.isEmpty() || chargeDuration == null) {
            return null;
        }
        List<List<BedrockItemAttachables.Stage>> prepared = new ArrayList<>(states.size());
        for (BedrockCrossbowStates.State state : states) {
            List<BedrockItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
            for (int i = 0; i < state.rules().size(); i++) {
                ItemBindingRule rule = state.rules().get(i);
                if (!rule.transformations().isEmpty()) {
                    return null;
                }
                ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                BufferedImage image = this.renderer.render(target);
                if (image == null) {
                    return null;
                }
                String texture = "textures/" + state.id().namespace() + '/' + state.id().value() + "/stage_" + i;
                stages.add(new BedrockItemAttachables.Stage(this.models.resolve(target.model()), image, texture, BedrockCrossbowStates.condition(rule), new Matrix4f()));
            }
            prepared.add(stages);
        }
        List<GeneratedItemVariant> variants = new ArrayList<>(states.size());
        for (int i = 0; i < states.size(); i++) {
            BedrockCrossbowStates.State state = states.get(i);
            List<BedrockItemAttachables.Stage> stages = prepared.get(i);
            for (BedrockItemAttachables.Stage stage : stages) {
                ImageUtils.writePng(stage.image(), context.root().resolve(stage.texture() + ".png"));
            }
            Bedrock2DCrossbowAttachables.write(state, stages, entry.useBehavior(), context.root());
            variants.add(GeneratedItemVariant.icon(state.id(), state.registration(), stages.get(state.idle()).texture(), true));
            context.playerAnimations().register2DCrossbow(state, chargeDuration.value());
        }
        return variants;
    }
}
