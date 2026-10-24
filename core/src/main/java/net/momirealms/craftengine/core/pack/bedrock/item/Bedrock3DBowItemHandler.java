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


public final class Bedrock3DBowItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public Bedrock3DBowItemHandler(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        int idle = BedrockItemAttachables.findBowIdle(rules);
        if (idle == -1) {
            return false;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(rules.size());
        ItemBindingRule idleRule = rules.get(idle);
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : rules) {
                Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
                BedrockModelTransform.uniformScale(transformation);
                ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                JavaModel model = this.models.resolve(target.model());
                JsonArray cubes = geometry.geometry(model, target);
                // 局部变换烘焙到几何，各阶段的手持 display 留给挂点骨骼应用。
                BedrockModelTransform.apply(cubes, transformation, 1);
                stages.add(new Bedrock3DItemAttachables.Stage(model, cubes, BedrockItemAttachables.bowCondition(rule)));
            }
            // 背包和掉落使用待机 display，手持几何不缩到方块范围。
            display = context.blocks().write(entry.bedrockId(), List.of(idleRule), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return false;
        }
        Bedrock3DBowAttachables.write(entry.bedrockId(), stages, geometry.materials(),
                entry.source().clientBehavior().use().maximumDuration().value(), context.root());
        List<ItemBindingCondition> conditions = idleRule.conditions().stream()
                .filter(condition -> !BedrockItemAttachables.isUseCondition(condition)).toList();
        ItemBindingRule registration = new ItemBindingRule(conditions, idleRule.target(), idleRule.transformations(), idleRule.sourcePath());
        context.variants().put(entry.bedrockId(), List.of(new BedrockItemIcons.Variant(entry.bedrockId(), registration, null)));
        context.displays().put(entry.bedrockId(), display);
        context.attachables().add(entry.bedrockId());
        context.bows().add(entry.bedrockId());
        context.javaBows().add(entry.bedrockId());
        return true;
    }

}
