package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

record BedrockItemPresentation(ResolvedItemBinding item,
                               Key id,
                               DisplayContextVariants.Variant variant,
                               EnumMap<ItemDisplayContext, ItemBindingRule> idleModels,
                               BedrockItemRenderStrategy strategy,
                               boolean explicitViews,
                               boolean separateWorldViews) {
    @Nullable
    static List<BedrockItemPresentation> plan(ResolvedItemBinding item, JavaModelResolver models) throws IOException {
        boolean explicitViews = DisplayContextVariants.usesDisplayContext(item);
        boolean separateWorldViews = explicitViews || item.definition().hasPlayerHead() || hasDifferentPartTransforms(item, models);
        List<? extends BedrockItemRenderStrategy> strategies = BedrockItemRenderStrategy.forItem(item);
        // 静态物品沿用普通图标
        if (!BedrockArmorResources.hasAsset(item) // 没有需要处理的装备外观资源
                && !separateWorldViews  // 不需要为掉落物、展示框、置物架分别生成外观
                && strategies.getFirst() == BedrockItemRenderStrategy.DEFAULT  // 没有启用弓、弩、盾、三叉戟等专用行为
                && item.useBehavior().animation() == ItemBehaviorDescription.UseAnimation.NONE) { // 不需要按使用状态生成外观
            return null;
        }

        Key base = item.bedrockId();
        List<BedrockItemPresentation> result = new ObjectArrayList<>();
        for (int i = 0, size1 = strategies.size(); i < size1; i++) {
            BedrockItemRenderStrategy strategy = strategies.get(i);
            List<DisplayContextVariants.Variant> variants = DisplayContextVariants.collect(item, strategy, explicitViews);
            for (int z = 0, size2 = variants.size(); z < size2; z++) {
                Key id = strategy.variantId(size2 == 1 ? base : base.withSameNamespace(base.value() + "_variant_" + z));
                DisplayContextVariants.Variant variant = variants.get(z);
                EnumMap<ItemDisplayContext, ItemBindingRule> idleModels = new EnumMap<>(ItemDisplayContext.class);
                for (ItemDisplayContext view : ItemDisplayContext.values()) {
                    idleModels.put(view, variant.idle(view, strategy));
                }
                result.add(new BedrockItemPresentation(item, id, variant, idleModels, strategy, explicitViews, separateWorldViews));
            }
        }
        return result;
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

    static boolean hasUseConditions(List<ItemBindingRule> rules) {
        for (ItemBindingRule rule : rules) {
            if (rule.hasCondition(BedrockItemAttachables::isUseCondition)) {
                return true;
            }
        }
        return false;
    }

    static Map<ItemDisplayContext, ItemBindingRule> ordinaryDisplayModels(ResolvedItemBinding item, List<ItemBindingCondition> conditions) {
        for (ItemBindingRule rule : item.definition().rules()) {
            if (!BedrockItemAttachables.matchesIdle(rule)) {
                continue;
            }
            boolean matches = true;
            for (ItemBindingCondition condition : rule.conditions()) {
                if (!BedrockItemAttachables.isUseCondition(condition) && !conditions.contains(condition)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                EnumMap<ItemDisplayContext, ItemBindingRule> models = new EnumMap<>(ItemDisplayContext.class);
                for (ItemDisplayContext view : ItemDisplayContext.values()) {
                    models.put(view, rule);
                }
                return models;
            }
        }
        return Map.of();
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
