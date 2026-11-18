package net.momirealms.craftengine.core.pack.bedrock.item.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockDyedItem;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.List;
import java.util.Map;

record BedrockItemPresentation(ResolvedItemBinding item,
                               Key id,
                               List<ItemBindingCondition> conditions,
                               Map<ItemDisplayContext, List<ItemBindingRule>> rulesByContext,
                               Map<ItemDisplayContext, ItemBindingRule> idleRules,
                               BedrockItemRenderStrategy strategy,
                               boolean explicitViews,
                               boolean separateWorldViews) {
    static Plan plan(ResolvedItemBinding item, JavaModelResolver models) throws IOException {
        boolean explicitViews = DisplayContextVariants.usesDisplayContext(item);
        boolean separateWorldViews = explicitViews || item.definition().hasPlayerHead() || hasDifferentPartTransforms(item, models);
        List<? extends BedrockItemRenderStrategy> strategies = BedrockItemRenderStrategy.forItem(item);
        boolean contextual = separateWorldViews
                || strategies.getFirst() != BedrockItemRenderStrategy.DEFAULT
                || item.useBehavior().animation() != ItemBehaviorDescription.UseAnimation.NONE
                || BedrockEquipmentResources.hasAsset(item)
                || hasUseConditions(item.definition().rules()) || BedrockDyedItem.hasDye(item.definition().rules())
                || requiresAffineBones(item.definition().rules());

        Key base = item.bedrockId();
        List<BedrockItemPresentation> result = new ObjectArrayList<>();
        // 大多数物品只有一个策略；弩按未装填、箭、烟花拆成三个策略，分别限定注册状态和渲染行为
        for (int i = 0, size1 = strategies.size(); i < size1; i++) {
            BedrockItemRenderStrategy strategy = strategies.get(i);
            // 将各显示位置的耐久、组件等物品栈条件求交，组合出可同时成立的注册变体
            // 一个变体包含所有显示位置的规则列表，拉弓等使用阶段仍保留在列表中，不在这里单独注册
            List<DisplayContextVariants.Variant> variants = DisplayContextVariants.collect(item, strategy, explicitViews);
            for (int z = 0, size2 = variants.size(); z < size2; z++) {
                Key id = strategy.variantId(size2 == 1 ? base : base.withSameNamespace(base.value() + "_variant_" + z));
                DisplayContextVariants.Variant variant = variants.get(z);
                result.add(new BedrockItemPresentation(item, id, variant.conditions(), variant.rules(), variant.idleRules(strategy), strategy, explicitViews, separateWorldViews));
            }
        }
        return new Plan(contextual, result);
    }

    static boolean hasUseConditions(List<ItemBindingRule> rules) {
        for (ItemBindingRule rule : rules) {
            if (rule.hasCondition(BedrockItemAttachables::isUseCondition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean requiresAffineBones(List<ItemBindingRule> rules) {
        for (ItemBindingRule rule : rules) {
            if (!rule.transformations().isEmpty() && !BedrockModelTransform.canBakeCubes(BedrockModelTransform.compose(rule.transformations()))) {
                return true;
            }
            if (rule.target() instanceof ItemRenderTarget.Composite composite && requiresAffineBones(composite.parts())) {
                return true;
            }
        }
        return false;
    }

    int sharedModelIdleIndex() {
        // 专用弓弩生成器要求所有位置共用一组普通模型；显式场景规则不能以 GUI 规则代替手持规则。
        if (this.explicitViews) {
            return -1;
        }
        ItemBindingRule idle = this.idleRules.get(ItemDisplayContext.GUI);
        List<ItemBindingRule> rules = this.rulesByContext.get(ItemDisplayContext.GUI);
        int index = -1;
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            if (!(rule.target() instanceof ItemRenderTarget.Model)) {
                return -1;
            }
            // idle 保留原规则的实例；这里定位已选中的分支，而非寻找结构相等的另一条规则。
            if (rule == idle) {
                index = i;
            }
        }
        return index;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private static boolean hasDifferentPartTransforms(ResolvedItemBinding item, JavaModelResolver models) throws IOException {
        for (ItemBindingRule rule : item.definition().rules()) {
            if (rule.target() instanceof ItemRenderTarget.Composite composite) {
                JavaModel first = null;
                for (ItemBindingRule part : composite.parts()) {
                    if (part.target() instanceof ItemRenderTarget.Model target) {
                        JavaModel model = target.resolve(models);
                        if (first == null) {
                            first = model;
                        } else if (!first.display().equals(model.display())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // contextual 决定生成路径；variants 为空表示没有输出，始终返回有效计划而非 null
    record Plan(boolean contextual, List<BedrockItemPresentation> variants) {
    }
}
