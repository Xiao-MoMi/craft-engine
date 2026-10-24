package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
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
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<BedrockCrossbowStates.State> states = BedrockCrossbowStates.collect(entry);
        if (states.isEmpty() || entry.source().clientBehavior().use().chargeDuration() == null) {
            return false;
        }
        List<List<BedrockItemAttachables.Stage>> prepared = new ArrayList<>(states.size());
        for (BedrockCrossbowStates.State state : states) {
            List<BedrockItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
            for (int i = 0; i < state.rules().size(); i++) {
                ItemBindingRule rule = state.rules().get(i);
                if (!rule.transformations().isEmpty()) {
                    return false;
                }
                ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                BufferedImage image = this.renderer.render(target);
                if (image == null) {
                    return false;
                }
                String texture = "textures/craftengine_icons/" + state.id().namespace() + '/' + state.id().value() + "_v" + i;
                stages.add(new BedrockItemAttachables.Stage(this.models.resolve(target.model()), image, texture, BedrockCrossbowStates.condition(rule), new Matrix4f()));
            }
            prepared.add(stages);
        }
        List<BedrockItemIcons.Variant> variants = new ArrayList<>(states.size());
        for (int i = 0; i < states.size(); i++) {
            BedrockCrossbowStates.State state = states.get(i);
            List<BedrockItemAttachables.Stage> stages = prepared.get(i);
            for (BedrockItemAttachables.Stage stage : stages) {
                ImageUtils.writePng(stage.image(), context.root().resolve(stage.texture() + ".png"));
            }
            Bedrock2DCrossbowAttachables.write(state, stages, entry.source().clientBehavior().use(), context.root());
            JsonObject icon = new JsonObject();
            icon.addProperty("textures", stages.get(state.idle()).texture());
            context.icons().add(state.id().asString(), icon);
            variants.add(new BedrockItemIcons.Variant(state.id(), state.registration(), null));
            context.attachables().add(state.id());
            if (!state.charged()) {
                context.crossbowPlayerAnimations().register(state.id(), entry.source().clientBehavior().use().chargeDuration().value());
            }
        }
        context.variants().put(entry.bedrockId(), variants);
        return true;
    }
}
