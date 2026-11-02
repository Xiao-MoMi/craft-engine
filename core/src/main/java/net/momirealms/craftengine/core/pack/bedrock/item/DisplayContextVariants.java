package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
        return entry.definition().rules().stream().flatMap(rule -> rule.conditions().stream()).anyMatch(DisplayContextVariants::isDisplayCondition);
    }

    static List<Variant> collect(ItemRegistrationPlan.Entry entry) {
        // Project the Java tree onto each view first. Views with identical branches only need to be partitioned once.
        Map<Map<List<ItemBindingCondition>, List<ItemBindingRule>>, List<ItemDisplayContext>> views = new LinkedHashMap<>();
        Map<ItemBindingCondition, String> unsupported = new HashMap<>();
        for (ItemBindingRule rule : entry.definition().rules()) {
            for (ItemBindingCondition condition : rule.conditions()) {
                if (!isDisplayCondition(condition) && !BedrockItemAttachables.isUseCondition(condition)) {
                    String property = unsupportedProperty(condition);
                    if (property != null) {
                        unsupported.put(condition, property);
                    }
                }
            }
        }
        for (ItemDisplayContext view : ItemDisplayContext.values()) {
            Map<List<ItemBindingCondition>, List<ItemBindingRule>> branches = new LinkedHashMap<>();
            Set<String> reported = new HashSet<>();
            JsonPrimitive value = new JsonPrimitive(BedrockItemDisplays.viewName(view));
            for (ItemBindingRule rule : entry.definition().rules()) {
                if (rule.conditions().stream().anyMatch(c -> isDisplayCondition(c) && !((ItemBindingCondition.SelectMatch) c).matches(value))) {
                    continue;
                }
                // Geyser cannot evaluate this property. Use its fallback in this view only.
                boolean skip = false;
                for (ItemBindingCondition condition : rule.conditions()) {
                    String property = unsupported.get(condition);
                    if (property != null) {
                        if (reported.add(property)) {
                            TranslationManager.warn("resource_pack.bedrock.item.display_skipped", entry.bedrockId().asString(), BedrockItemDisplays.viewName(view), property);
                        }
                        skip |= !fallback(condition);
                    }
                }
                if (skip) {
                    continue;
                }
                List<ItemBindingCondition> stack = rule.conditions().stream()
                        .filter(c -> !isDisplayCondition(c) && !BedrockItemAttachables.isUseCondition(c) && !unsupported.containsKey(c)).toList();
                branches.computeIfAbsent(stack, ignored -> new ArrayList<>()).add(rule);
            }
            if (branches.isEmpty()) {
                branches.put(List.of(), List.of(EMPTY));
            }
            views.computeIfAbsent(branches, ignored -> new ArrayList<>()).add(view);
        }
        List<Variant> variants = List.of(new Variant(List.of(), new EnumMap<>(ItemDisplayContext.class)));
        for (Map.Entry<Map<List<ItemBindingCondition>, List<ItemBindingRule>>, List<ItemDisplayContext>> group : views.entrySet()) {
            // A durability split may exist in only one view. Intersect its ranges with the other views' stack conditions.
            List<Variant> combined = new ArrayList<>();
            for (Variant variant : variants) {
                for (Map.Entry<List<ItemBindingCondition>, List<ItemBindingRule>> branch : group.getKey().entrySet()) {
                    List<ItemBindingCondition> stack = ItemConditionIntersection.intersect(variant.conditions(), branch.getKey());
                    if (stack != null) {
                        EnumMap<ItemDisplayContext, List<ItemBindingRule>> rules = new EnumMap<>(variant.rules());
                        for (ItemDisplayContext view : group.getValue()) {
                            rules.put(view, branch.getValue());
                        }
                        combined.add(new Variant(stack, rules));
                    }
                }
            }
            variants = combined;
        }
        return variants;
    }

    private static boolean isDisplayCondition(ItemBindingCondition condition) {
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
            case "broken", "damaged", "selected", "fishing_rod/cast", "trim_material", "context_dimension", "charge_type",
                    "damage", "count", "bundle/fullness" -> null;
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

    record Variant(List<ItemBindingCondition> conditions, EnumMap<ItemDisplayContext, List<ItemBindingRule>> rules) {
        ItemBindingRule idle(ItemDisplayContext view) {
            return this.rules.get(view).stream().filter(BedrockItemAttachables::matchesIdle).findFirst().orElse(EMPTY);
        }
    }
}
