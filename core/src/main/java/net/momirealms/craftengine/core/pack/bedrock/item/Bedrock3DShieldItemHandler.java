package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Bedrock3DShieldItemHandler implements BedrockItemHandler {
    private final JavaModelResources resources;

    public Bedrock3DShieldItemHandler(JavaModelResources resources) {
        this.resources = resources;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        ItemBindingRule idle = null;
        for (ItemBindingRule rule : rules) {
            if (!(rule.target() instanceof ItemRenderTarget.Model)) {
                return null;
            }
            if (idle == null && rule.conditions().stream().noneMatch(condition -> condition instanceof ItemBindingCondition.BooleanMatch match
                    && match.property() instanceof UsingItemConditionProperty && match.expected())) {
                idle = rule;
            }
        }
        if (idle == null || ItemRenderType.classify(entry.definition(), this.resources) != ItemRenderType.BLOCK) {
            return null;
        }
        List<ItemBindingCondition> conditions = idle.conditions().stream().filter(condition -> !isUsingItem(condition)).toList();
        for (ItemBindingRule rule : rules) {
            if (!conditions.equals(rule.conditions().stream().filter(condition -> !isUsingItem(condition)).toList())) {
                return null;
            }
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(this.resources.models(), this.resources.textures());
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(rules.size());
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : rules) {
                StringBuilder condition = new StringBuilder("1");
                for (ItemBindingCondition value : rule.conditions()) {
                    if (value instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty) {
                        condition.append(match.expected()
                                ? " && variable.ce_using_item" : " && !variable.ce_using_item");
                    }
                }
                stages.add(geometry.createStage(rule, condition.toString()));
            }
            display = context.itemDisplayBlocks().write(entry.bedrockId(), List.of(idle), this.resources.models(), this.resources.textures(), context.root());
        } catch (UnsupportedOperationException exception) {
            return null;
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
        context.playerAnimations().registerShieldUsingJavaPose(entry.bedrockId());
        return List.of(GeneratedItemVariant.block(entry.bedrockId(), registration, display, true));
    }

    private static boolean isUsingItem(ItemBindingCondition condition) {
        return condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty;
    }
}
