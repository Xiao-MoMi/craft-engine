package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.*;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

import java.util.*;

final class DisplayContextVariants {
    private static final ItemBindingRule EMPTY = new ItemBindingRule(List.of(), ItemRenderTarget.Builtin.EMPTY, List.of(), "");

    private DisplayContextVariants() {
    }

    static boolean usesDisplayContext(ResolvedItemBinding entry) {
        for (ItemBindingRule rule : entry.definition().rules()) {
            if (rule.hasCondition(DisplayContextVariants::isDisplayCondition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 Java 模型规则按物品栈条件划分为基岩物品变体。每个变体保存各显示位置可用的模型规则
     * 例如，只有手持模型随耐久变化时，会按耐久划分变体，但这些变体仍共用同一个背包模型
     */
    static List<Variant> collect(ResolvedItemBinding entry, BedrockItemRenderStrategy strategy, boolean explicitViews) {
        List<PreparedRule> rules = prepareRules(entry.definition().rules(), strategy);
        Map<ViewBranches, List<ItemDisplayContext>> contextsByBranches = new LinkedHashMap<>();
        if (!explicitViews) {
            // Java 没有按显示位置分支时，只对公共规则做一次分组
            ViewBranches shared = branchesForView(entry, rules, ItemDisplayContext.GUI);
            contextsByBranches.put(shared, List.of(ItemDisplayContext.values()));
        } else {
            // 分支内容相同的显示位置共用一组输入，避免重复参与后面的条件交集组合
            for (ItemDisplayContext context : ItemDisplayContext.values()) {
                ViewBranches branches = branchesForView(entry, rules, context);
                contextsByBranches.computeIfAbsent(branches, ignored -> new ObjectArrayList<>(1)).add(context);
            }
        }

        // 先固定策略自身的注册状态（如弩的装填类型），再逐组加入各显示位置的物品栈条件
        List<ItemBindingCondition> initial = ItemConditionIntersection.intersect(strategy.stateConditions(), entry.definition().identityConditions());
        if (initial == null) {
            return List.of();
        }
        List<Variant> variants = List.of(new Variant(initial, new EnumMap<>(ItemDisplayContext.class)));
        for (Map.Entry<ViewBranches, List<ItemDisplayContext>> group : contextsByBranches.entrySet()) {
            variants = mergeVariants(variants, group.getKey(), group.getValue());
        }
        return variants;
    }

    private static List<PreparedRule> prepareRules(List<ItemBindingRule> rules, BedrockItemRenderStrategy strategy) {
        List<PreparedRule> prepared = new ArrayList<>(rules.size());
        for (ItemBindingRule rule : rules) {
            if (!strategy.accepts(rule)) {
                continue;
            }
            List<ItemBindingCondition> stackConditions = new ObjectArrayList<>();
            List<String> unsupportedProperties = new ObjectArrayList<>();
            boolean matchesFallback = true;
            for (ItemBindingCondition condition : rule.conditions()) {
                // 原规则保留完整条件。分组键只收集物品栈条件，显示位置用于筛选规则，使用状态留给渲染阶段
                if (isDisplayCondition(condition) || strategy.isEquipmentCondition(condition)) {
                    continue;
                }
                // 是否为不支持的 condition 类型
                String property = unsupportedProperty(condition);
                if (property == null) {
                    stackConditions.add(condition);
                } else {
                    unsupportedProperties.add(property);
                    matchesFallback &= fallback(condition);
                }
            }
            prepared.add(new PreparedRule(rule, stackConditions, unsupportedProperties, matchesFallback));
        }
        return prepared;
    }

    private static ViewBranches branchesForView(ResolvedItemBinding entry, List<PreparedRule> rules, ItemDisplayContext context) {
        // 例如右手模型在损耗低于 200 时的松弦、拉弦规则同属一组，按 Java 分支顺序交给阶段选择器
        Map<List<ItemBindingCondition>, List<ItemBindingRule>> branches = new LinkedHashMap<>();
        String viewName = context.serializedName();
        JsonPrimitive view = new JsonPrimitive(viewName);
        Set<String> reported = new ObjectOpenHashSet<>();
        for (PreparedRule rule : rules) {
            if (!matchesView(rule.binding(), view)) {
                continue;
            }
            // 每种不支持的属性在当前显示位置只警告一次，即使触发警告的规则随后被跳过
            for (String property : rule.unsupportedProperties()) {
                if (reported.add(property)) {
                    TranslationManager.warn("resource_pack.bedrock.item.condition_fallback", entry.bedrockId().asString(), viewName, property);
                }
            }
            if (rule.matchesFallback()) {
                branches.computeIfAbsent(rule.stackConditions(), ignored -> new ObjectArrayList<>(1)).add(rule.binding());
            }
        }
        if (branches.isEmpty()) {
            // 这个位置没有可用模型。保留空模型分支，让其他位置的模型仍能参与组合
            branches.put(List.of(), List.of(EMPTY));
        }
        return new ViewBranches(branches);
    }

    private static List<Variant> mergeVariants(List<Variant> variants, ViewBranches branches, List<ItemDisplayContext> contexts) {
        List<Variant> combined = new ObjectArrayList<>();
        for (Variant variant : variants) {
            for (Map.Entry<List<ItemBindingCondition>, List<ItemBindingRule>> branch : branches.byStackConditions().entrySet()) {
                // 同一物品栈需要满足各位置的分支条件。背包分支要求损耗至少 100、手持分支要求低于 200，交集为 [100, 200)
                // 背包分支要求低于 100、手持分支要求至少 200 时，交集为空，丢弃这组搭配
                List<ItemBindingCondition> conditions = ItemConditionIntersection.intersect(variant.conditions(), branch.getKey());
                if (conditions == null) {
                    continue;
                }
                EnumMap<ItemDisplayContext, List<ItemBindingRule>> viewRules = new EnumMap<>(variant.rules());
                for (ItemDisplayContext context : contexts) {
                    viewRules.put(context, branch.getValue());
                }
                combined.add(new Variant(conditions, viewRules));
            }
        }
        return combined;
    }

    private static boolean matchesView(ItemBindingRule rule, JsonPrimitive view) {
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof DisplayContextSelectProperty && !select.matches(view)) {
                return false;
            }
        }
        return true;
    }

    static boolean isDisplayCondition(ItemBindingCondition condition) {
        return condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof DisplayContextSelectProperty;
    }

    private static String unsupportedProperty(ItemBindingCondition condition) {
        JsonObject json = new JsonObject();
        switch (condition) {
            case ItemBindingCondition.BooleanMatch match -> match.property().writeProperty(json);
            case ItemBindingCondition.SelectMatch match -> match.property().writeProperty(json);
            case ItemBindingCondition.NumericRange match -> match.property().writeProperty(json);
        }
        String property = json.get("property").getAsString();
        return switch (property) {
            case "broken", "damaged", "selected", "fishing_rod/cast", "trim_material", "context_dimension", "charge_type", "damage", "count", "bundle/fullness", "custom_model_data" -> null;
            case "has_component" -> json.has("ignore_default") && json.get("ignore_default").getAsBoolean() ? property : null;
            default -> property;
        };
    }

    private static boolean fallback(ItemBindingCondition condition) {
        // 无法在基岩侧求值的属性按默认状态选择：布尔取 false，select 走排除分支，数值取 0。
        // 不能简单删除这些条件，否则原本互斥的模型分支可能被同时保留。
        return switch (condition) {
            case ItemBindingCondition.BooleanMatch match -> !match.expected();
            case ItemBindingCondition.SelectMatch match -> match.negated();
            case ItemBindingCondition.NumericRange range -> range.matches(0);
        };
    }

    private record PreparedRule(ItemBindingRule binding,
                                List<ItemBindingCondition> stackConditions,
                                List<String> unsupportedProperties,
                                boolean matchesFallback) {
    }

    private record ViewBranches(Map<List<ItemBindingCondition>, List<ItemBindingRule>> byStackConditions) {
    }

    record Variant(List<ItemBindingCondition> conditions, // 哪种物品栈会选中这个变体
                   EnumMap<ItemDisplayContext, List<ItemBindingRule>> rules) { // 选中这个变体后，各个显示位置用哪些模型规则

        EnumMap<ItemDisplayContext, ItemBindingRule> idleRules(BedrockItemRenderStrategy strategy) {
            EnumMap<ItemDisplayContext, ItemBindingRule> result = new EnumMap<>(ItemDisplayContext.class);
            Map<List<ItemBindingRule>, ItemBindingRule> selected = new IdentityHashMap<>();
            // 按策略选中第一条闲置规则，供图标和展示实体使用。共享同一列表的场景只需扫描一次
            for (Map.Entry<ItemDisplayContext, List<ItemBindingRule>> entry : this.rules.entrySet()) {
                ItemBindingRule idle = selected.get(entry.getValue());
                if (idle == null) {
                    idle = EMPTY;
                    for (ItemBindingRule rule : entry.getValue()) {
                        if (strategy.matchesIdle(rule)) {
                            idle = rule;
                            break;
                        }
                    }
                    selected.put(entry.getValue(), idle);
                }
                result.put(entry.getKey(), idle);
            }
            return result;
        }
    }
}
