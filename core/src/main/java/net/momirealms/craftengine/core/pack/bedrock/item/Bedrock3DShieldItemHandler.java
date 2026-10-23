package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Bedrock3DShieldItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public Bedrock3DShieldItemHandler(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        ItemBindingRule idle = null;
        for (ItemBindingRule rule : rules) {
            if (!(rule.target() instanceof ItemRenderTarget.Model)) {
                return false;
            }
            if (idle == null && rule.conditions().stream().noneMatch(condition -> condition instanceof ItemBindingCondition.BooleanMatch match
                    && match.property() instanceof UsingItemConditionProperty && match.expected())) {
                idle = rule;
            }
        }
        if (idle == null || this.models.classify(entry.definition()) != JavaModelResolver.RenderType.BLOCK) {
            return false;
        }
        List<ItemBindingCondition> conditions = idle.conditions().stream().filter(condition -> !isUsingItem(condition)).toList();
        for (ItemBindingRule rule : rules) {
            if (!conditions.equals(rule.conditions().stream().filter(condition -> !isUsingItem(condition)).toList())) {
                return false;
            }
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.models, this.textures);
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(rules.size());
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : rules) {
                ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                JavaModel model = this.models.resolve(target.model());
                Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
                BedrockModelTransform.uniformScale(transformation);
                JsonArray cubes = geometry.geometry(model, target);
                BedrockModelTransform.apply(cubes, transformation, 1);
                StringBuilder condition = new StringBuilder("1");
                for (ItemBindingCondition value : rule.conditions()) {
                    if (value instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty) {
                        condition.append(match.expected()
                                ? " && variable.ce_using_item" : " && !variable.ce_using_item");
                    }
                }
                stages.add(new Bedrock3DItemAttachables.Stage(model, cubes, condition.toString()));
            }
            display = context.blocks().write(entry.bedrockId(), List.of(idle), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return false;
        }
        JsonArray scripts = new JsonArray();
        // Geyser 将盾牌使用映射为 BLOCKING，并用 EMERGING 标记实际使用的副手。
        scripts.add("variable.ce_using_item = query.blocking && ((context.item_slot == 'main_hand' && !query.is_emerging) || (context.item_slot == 'off_hand' && query.is_emerging));");
        String selection = "-1";
        for (int i = stages.size() - 1; i >= 0; i--) {
            selection = "(" + stages.get(i).condition() + ") ? " + i + " : (" + selection + ")";
        }
        scripts.add("variable.ce_stage = " + selection + ";");
        // Java ShieldItem 的第一人称 BLOCK 分支不追加旋转，直接使用当前模型的 display。
        Bedrock3DItemAttachables.write(entry.bedrockId(), stages, geometry.materials(), scripts,
                new JsonArray(), new JsonObject(), new JsonArray(), context.root());
        ItemBindingRule registration = new ItemBindingRule(conditions, idle.target(), idle.transformations(), idle.sourcePath());
        context.variants().put(entry.bedrockId(), List.of(new BedrockItemIcons.Variant(entry.bedrockId(), entry.icon(), registration, null)));
        context.displays().put(entry.bedrockId(), display);
        context.attachables().add(entry.bedrockId());
        context.javaShields().add(entry.bedrockId());
        return true;
    }

    private static boolean isUsingItem(ItemBindingCondition condition) {
        return condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty;
    }
}
