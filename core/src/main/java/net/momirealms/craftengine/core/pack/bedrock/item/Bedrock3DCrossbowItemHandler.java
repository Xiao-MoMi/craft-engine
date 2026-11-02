package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;

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
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<BedrockCrossbowStates.State> states = BedrockCrossbowStates.collect(entry);
        if (states.isEmpty() || entry.source().clientBehavior().use().chargeDuration() == null) {
            return null;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<List<Bedrock3DItemAttachables.Stage>> prepared = new ArrayList<>(states.size());
        List<BedrockItemBlocks.DisplayBlock> displays = new ArrayList<>(states.size());
        try {
            for (BedrockCrossbowStates.State state : states) {
                List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
                for (ItemBindingRule rule : state.rules()) {
                    stages.add(geometry.createStage(rule, BedrockCrossbowStates.condition(rule)));
                }
                prepared.add(stages);
            }
            for (BedrockCrossbowStates.State state : states) {
                displays.add(context.blocks().write(state.id(), List.of(state.rules().get(state.idle())), this.models, this.textures, context.root()));
            }
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        List<GeneratedItemVariant> variants = new ArrayList<>(states.size());
        for (int i = 0; i < states.size(); i++) {
            BedrockCrossbowStates.State state = states.get(i);
            Bedrock3DCrossbowAttachables.write(state.id(), prepared.get(i), geometry.materials(), state, entry.source().clientBehavior().use(), context.root());
            variants.add(GeneratedItemVariant.block(state.id(), state.registration(), displays.get(i), true));
            context.crossbowPlayerAnimations().register(state.id(), state.charged(), entry.source().clientBehavior().use().chargeDuration().value(), true);
        }
        return variants;
    }
}
