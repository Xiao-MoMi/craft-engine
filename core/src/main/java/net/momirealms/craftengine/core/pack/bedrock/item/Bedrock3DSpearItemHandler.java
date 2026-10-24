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

public final class Bedrock3DSpearItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public Bedrock3DSpearItemHandler(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        BedrockSpearModels selected = BedrockSpearModels.collect(entry);
        if (selected == null) {
            return false;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>();
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (int i = 0; i < selected.hands().size(); i++) {
                ItemBindingRule rule = selected.hands().get(i);
                Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
                BedrockModelTransform.uniformScale(transformation);
                ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                JavaModel model = this.models.resolve(target.model());
                JsonArray cubes = geometry.geometry(model, target);
                BedrockModelTransform.apply(cubes, transformation, 1);
                stages.add(new Bedrock3DItemAttachables.Stage(model, cubes, selected.condition(i)));
            }
            display = context.blocks().write(entry.bedrockId(), List.of(selected.inventory()), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return false;
        }
        Bedrock3DSpearAttachables.write(entry, selected, stages, geometry.materials(), context.root());
        context.displays().put(entry.bedrockId(), display);
        context.attachables().add(entry.bedrockId());
        context.javaSpears().add(entry.bedrockId());
        context.variants().put(entry.bedrockId(), List.of(new BedrockItemIcons.Variant(entry.bedrockId(), selected.registration(), null)));
        return true;
    }
}
