package net.momirealms.craftengine.core.pack.bedrock.item;

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
            display = context.blocks().write(entry.bedrockId(), List.of(selected.inventory()), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        Bedrock3DSpearAttachables.write(entry, selected, stages, geometry.materials(), context.root());
        context.javaSpears().put(entry.bedrockId(), entry);
        return List.of(GeneratedItemVariant.block(entry.bedrockId(), selected.registration(), display, true));
    }
}
