package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DisplayContextVariants {
    private static final ItemBindingRule EMPTY = new ItemBindingRule(List.of(), ItemRenderTarget.Builtin.EMPTY, List.of(), "");

    private DisplayContextVariants() {
    }

    static boolean usesDisplayContext(ItemRegistrationPlan.Entry entry) {
        for (ItemBindingRule rule : entry.definition().rules()) {
            if (rule.hasCondition(DisplayContextVariants::isDisplayCondition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 Java 模型规则按物品栈条件划分为基岩物品变体。每个变体保存各显示位置可用的模型规则。
     * 例如，只有手持模型随耐久变化时，会按耐久划分变体，但这些变体仍共用同一个背包模型。
     */
    static List<Variant> collect(ItemRegistrationPlan.Entry entry) {
        List<PreparedRule> rules = prepareRules(entry.definition().rules());
        Map<ViewBranches, List<ItemDisplayContext>> contextsByBranches = new LinkedHashMap<>();
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            ViewBranches branches = branchesForView(entry, rules, context);
            contextsByBranches.computeIfAbsent(branches, ignored -> new ObjectArrayList<>(1)).add(context);
        }

        List<Variant> variants = List.of(new Variant(List.of(), new EnumMap<>(ItemDisplayContext.class)));
        for (Map.Entry<ViewBranches, List<ItemDisplayContext>> group : contextsByBranches.entrySet()) {
            variants = mergeVariants(variants, group.getKey(), group.getValue());
        }
        return variants;
    }

    private static List<PreparedRule> prepareRules(List<ItemBindingRule> rules) {
        List<PreparedRule> prepared = new ArrayList<>(rules.size());
        for (ItemBindingRule rule : rules) {
            List<ItemBindingCondition> stackConditions = new ObjectArrayList<>();
            List<String> unsupportedProperties = new ObjectArrayList<>();
            boolean matchesFallback = true;
            for (ItemBindingCondition condition : rule.conditions()) {
                // 显示位置由 branchesForView 筛选，使用状态由手持模型处理，两者都不区分物品变体。
                if (isDisplayCondition(condition) || BedrockItemAttachables.isUseCondition(condition)) {
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

    private static ViewBranches branchesForView(ItemRegistrationPlan.Entry entry, List<PreparedRule> rules, ItemDisplayContext context) {
        Map<List<ItemBindingCondition>, List<ItemBindingRule>> branches = new LinkedHashMap<>();
        String viewName = context.serializedName();
        JsonPrimitive view = new JsonPrimitive(viewName);
        Set<String> reported = new ObjectOpenHashSet<>();
        for (PreparedRule rule : rules) {
            if (!matchesView(rule.binding(), view)) {
                continue;
            }
            // 每种不支持的属性在当前显示位置只警告一次，即使触发警告的规则随后被跳过。
            for (String property : rule.unsupportedProperties()) {
                if (reported.add(property)) {
                    TranslationManager.warn("resource_pack.bedrock.item.display_skipped", entry.bedrockId().asString(), viewName, property);
                }
            }
            if (rule.matchesFallback()) {
                branches.computeIfAbsent(rule.stackConditions(), ignored -> new ObjectArrayList<>(1)).add(rule.binding());
            }
        }
        if (branches.isEmpty()) {
            // 这个位置没有可用模型。保留空模型分支，让其他位置的模型仍能参与组合。
            branches.put(List.of(), List.of(EMPTY));
        }
        return new ViewBranches(branches);
    }

    private static List<Variant> mergeVariants(List<Variant> variants, ViewBranches branches, List<ItemDisplayContext> contexts) {
        List<Variant> combined = new ObjectArrayList<>();
        for (Variant variant : variants) {
            for (Map.Entry<List<ItemBindingCondition>, List<ItemBindingRule>> branch : branches.byStackConditions().entrySet()) {
                // 只有能同时满足各显示位置条件的物品栈，才需要一个共同的注册变体。
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

    private record PreparedRule(ItemBindingRule binding,
                                List<ItemBindingCondition> stackConditions,
                                List<String> unsupportedProperties,
                                boolean matchesFallback) {
    }

    private record ViewBranches(Map<List<ItemBindingCondition>, List<ItemBindingRule>> byStackConditions) {
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
            case "broken", "damaged", "selected", "fishing_rod/cast", "trim_material", "context_dimension", "charge_type", "damage", "count", "bundle/fullness" -> null;
            case "has_component" -> json.has("ignore_default") && json.get("ignore_default").getAsBoolean() ? property : null;
            default -> property;
        };
    }

    private static boolean fallback(ItemBindingCondition condition) {
        return switch (condition) {
            case ItemBindingCondition.BooleanMatch match -> !match.expected();
            case ItemBindingCondition.SelectMatch match -> match.negated();
            case ItemBindingCondition.NumericRange range -> range.matches(0);
        };
    }

    record Variant(List<ItemBindingCondition> conditions, // 哪种物品栈会选中这个变体
                   EnumMap<ItemDisplayContext, List<ItemBindingRule>> rules // 选中这个变体后，各个显示位置用哪些模型规则
    ) {

        ItemBindingRule idle(ItemDisplayContext view) {
            for (ItemBindingRule rule : this.rules.get(view)) {
                if (BedrockItemAttachables.matchesIdle(rule)) {
                    return rule;
                }
            }
            return EMPTY;
        }
    }
}
