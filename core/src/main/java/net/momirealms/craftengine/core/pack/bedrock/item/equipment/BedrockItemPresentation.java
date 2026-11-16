package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

record BedrockItemPresentation(ResolvedItemBinding item,
                               Key id,
                               ItemBindingRule registration,
                               DisplayContextVariants.Variant variant,
                               EnumMap<ItemDisplayContext, ItemBindingRule> idleModels,
                               BedrockEquipmentBehavior equipment,
                               boolean explicitViews,
                               boolean separateWorldViews) {
    @Nullable
    static List<BedrockItemPresentation> plan(ResolvedItemBinding item) {
        boolean explicitViews = DisplayContextVariants.usesDisplayContext(item);
        boolean separateWorldViews = explicitViews || item.definition().hasPlayerHead();
        List<? extends BedrockEquipmentBehavior> behaviors = BedrockEquipmentBehavior.forItem(item, separateWorldViews);
        // 静态物品沿用普通图标
        if (!BedrockArmorResources.hasAsset(item) // 没有需要处理的装备外观资源
                && !separateWorldViews  // 不需要为掉落物、展示框、置物架分别生成外观
                && behaviors.getFirst() == BedrockEquipmentBehavior.DEFAULT  // 没有启用弓、弩、盾、三叉戟等专用行为
                && (item.useBehavior().animation() == ItemBehaviorDescription.UseAnimation.NONE || !hasUseConditions(item.definition().rules()))) { // 不需要按使用状态生成外观
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
                result.add(new BedrockItemPresentation(item, id, registration, variant, idleModels, behavior, explicitViews, separateWorldViews));
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
