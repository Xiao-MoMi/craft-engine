package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Bedrock3DCrossbowItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public Bedrock3DCrossbowItemHandler(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<BedrockCrossbowStates.State> states = BedrockCrossbowStates.collect(entry);
        if (states.isEmpty() || entry.source().clientBehavior().use().chargeDuration() == null) {
            return false;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<List<Bedrock3DItemAttachables.Stage>> prepared = new ArrayList<>(states.size());
        List<BedrockItemBlocks.DisplayBlock> displays = new ArrayList<>(states.size());
        try {
            for (BedrockCrossbowStates.State state : states) {
                List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
                for (ItemBindingRule rule : state.rules()) {
                    Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
                    BedrockModelTransform.uniformScale(transformation);
                    ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                    JavaModel model = this.models.resolve(target.model());
                    JsonArray cubes = geometry.geometry(model, target);
                    BedrockModelTransform.apply(cubes, transformation, 1);
                    stages.add(new Bedrock3DItemAttachables.Stage(model, cubes, BedrockCrossbowStates.condition(rule)));
                }
                prepared.add(stages);
            }
            for (BedrockCrossbowStates.State state : states) {
                displays.add(context.blocks().write(state.id(), List.of(state.rules().get(state.idle())), this.models, this.textures, context.root()));
            }
        } catch (UnsupportedOperationException exception) {
            return false;
        }
        List<BedrockItemIcons.Variant> variants = new ArrayList<>(states.size());
        for (int i = 0; i < states.size(); i++) {
            BedrockCrossbowStates.State state = states.get(i);
            Bedrock3DCrossbowAttachables.write(state.id(), prepared.get(i), geometry.materials(), state, entry.source().clientBehavior().use(), context.root());
            variants.add(new BedrockItemIcons.Variant(state.id(), state.registration(), null));
            context.displays().put(state.id(), displays.get(i));
            context.attachables().add(state.id());
            context.crossbowPlayerAnimations().register(state.id(), state.charged(), entry.source().clientBehavior().use().chargeDuration().value(), true);
        }
        context.variants().put(entry.bedrockId(), variants);
        return true;
    }
}
