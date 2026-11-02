package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;

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
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        int idle = BedrockItemAttachables.findBowIdle(rules);
        if (idle == -1) {
            return null;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(rules.size());
        ItemBindingRule idleRule = rules.get(idle);
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : rules) {
                stages.add(geometry.createStage(rule, BedrockItemAttachables.bowCondition(rule)));
            }
            // 背包和掉落使用待机 display，手持几何不缩到方块范围。
            display = context.blocks().write(entry.bedrockId(), List.of(idleRule), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        Bedrock3DBowAttachables.write(entry.bedrockId(), stages, geometry.materials(),
                entry.source().clientBehavior().use().maximumDuration().value(), context.root());
        List<ItemBindingCondition> conditions = idleRule.conditions().stream()
                .filter(condition -> !BedrockItemAttachables.isUseCondition(condition)).toList();
        ItemBindingRule registration = new ItemBindingRule(conditions, idleRule.target(), idleRule.transformations(), idleRule.sourcePath());
        context.bows().add(entry.bedrockId());
        context.javaBows().add(entry.bedrockId());
        return List.of(GeneratedItemVariant.block(entry.bedrockId(), registration, display, true));
    }

}
