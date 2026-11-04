package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

record BedrockItemPresentation(ItemRegistrationPlan.Entry item,
                               Key id,
                               ItemBindingRule registration,
                               DisplayContextVariants.Variant variant,
                               EnumMap<ItemDisplayContext, ItemBindingRule> idleModels,
                               BedrockEquipmentBehavior equipment,
                               boolean explicitViews,
                               boolean worldAliases) {
    @Nullable
    static List<BedrockItemPresentation> plan(ItemRegistrationPlan.Entry item) {
        boolean explicitViews = DisplayContextVariants.usesDisplayContext(item);
        boolean worldAliases = explicitViews || item.definition().hasPlayerHead();
        List<? extends BedrockEquipmentBehavior> behaviors = BedrockEquipmentBehavior.forItem(item, worldAliases);
        if (!worldAliases && behaviors.getFirst() == BedrockEquipmentBehavior.DEFAULT && (item.useBehavior().animation() == ItemBehaviorDescription.UseAnimation.NONE || !hasUseConditions(item.definition().rules()))) {
            // 静态物品沿用普通图标 / 方块路径，保留其染色、变体优先级和跳过原因。
            return null;
        }

        Key base = item.bedrockId();
        List<BedrockItemPresentation> result = new ArrayList<>();
        for (BedrockEquipmentBehavior behavior : behaviors) {
            List<DisplayContextVariants.Variant> variants = DisplayContextVariants.collect(item, behavior, explicitViews);
            for (int i = 0; i < variants.size(); i++) {
                Key id = behavior.variantId(variants.size() == 1 ? base : Key.of(base.namespace(), base.value() + "_variant_" + i));
                DisplayContextVariants.Variant variant = variants.get(i);
                EnumMap<ItemDisplayContext, ItemBindingRule> idleModels = new EnumMap<>(ItemDisplayContext.class);
                for (ItemDisplayContext view : ItemDisplayContext.values()) {
                    idleModels.put(view, variant.idle(view, behavior));
                }
                ItemBindingRule gui = idleModels.get(ItemDisplayContext.GUI);
                ItemBindingRule registration = new ItemBindingRule(variant.conditions(), gui.target(), gui.transformations(), gui.sourcePath());
                result.add(new BedrockItemPresentation(item, id, registration, variant, idleModels, behavior, explicitViews, worldAliases));
            }
        }
        return result;
    }

    static boolean hasUseConditions(List<ItemBindingRule> rules) {
        for (ItemBindingRule rule : rules) {
            if (rule.hasCondition(BedrockItemAttachables::isUseCondition)) {
                return true;
            }
        }
        return false;
    }

    int sharedModelIdleIndex() {
        ItemBindingRule idle = this.idleModels.get(ItemDisplayContext.GUI);
        List<ItemBindingRule> rules = this.sharedRules();
        int index = -1;
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            if (!(rule.target() instanceof ItemRenderTarget.Model)) {
                return -1;
            }
            if (rule == idle) {
                index = i;
            }
        }
        return index;
    }

    List<ItemBindingRule> sharedRules() {
        return this.variant.rules().get(ItemDisplayContext.GUI);
    }
}
