package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ItemConditionIntersection {
    private ItemConditionIntersection() {
    }

    @Nullable
    public static List<ItemBindingCondition> intersect(List<ItemBindingCondition> conditions, List<ItemBindingCondition> additional) {
        if (additional.isEmpty()) {
            return conditions;
        }
        List<ItemBindingCondition> result = new ArrayList<>(conditions);
        for (ItemBindingCondition next : additional) {
            if (!addCondition(result, next)) {
                return null;
            }
        }
        return result;
    }

    /**
     * 将新条件并入当前分支的条件列表。遇到冲突时返回 false，调用方会丢弃整个分支。
     */
    @SuppressWarnings("DeconstructionCanBeUsed")
    private static boolean addCondition(List<ItemBindingCondition> conditions, ItemBindingCondition next) {
        // scale 为零时只可能得到零；要求命中空候选集的 select 也没有可匹配的值。
        if (next instanceof ItemBindingCondition.NumericRange range && range.scale() == 0 && !range.matches(0)) {
            return false;
        }
        if (next instanceof ItemBindingCondition.SelectMatch select && !select.negated() && select.values().isEmpty()) {
            return false;
        }
        for (int i = 0; i < conditions.size(); i++) {
            ItemBindingCondition existing = conditions.get(i);
            if (existing.equals(next)) {
                return true;
            }
            if (existing instanceof ItemBindingCondition.BooleanMatch previous
                    && next instanceof ItemBindingCondition.BooleanMatch incoming
                    && previous.property().equals(incoming.property())) {
                return previous.expected() == incoming.expected();
            }
            if (existing instanceof ItemBindingCondition.SelectMatch previous
                    && next instanceof ItemBindingCondition.SelectMatch incoming
                    && previous.property().equals(incoming.property())) {
                // 两个 select 都排除某些值时，合并排除项；只要一方限定了候选值，就只保留双方都允许的值。
                ItemBindingCondition.SelectMatch intersection = intersectSelect(previous, incoming);
                if (!intersection.negated() && intersection.values().isEmpty()) {
                    return false;
                }
                conditions.set(i, intersection);
                return true;
            }
            if (existing instanceof ItemBindingCondition.NumericRange previous
                    && next instanceof ItemBindingCondition.NumericRange incoming
                    && previous.property().equals(incoming.property())
                    && previous.scale() == incoming.scale()) {
                // 相同 scale 的区间可以直接取交集。下界包含、上界排除，因此两端相等时也没有交集。
                Float min = previous.minInclusive();
                Float max = previous.maxExclusive();
                if (incoming.minInclusive() != null && (min == null || min < incoming.minInclusive())) {
                    min = incoming.minInclusive();
                }
                if (incoming.maxExclusive() != null && (max == null || max > incoming.maxExclusive())) {
                    max = incoming.maxExclusive();
                }
                if (min != null && max != null && min >= max) {
                    return false;
                }
                ItemBindingCondition.NumericRange intersection = new ItemBindingCondition.NumericRange(previous.property(), previous.scale(), min, max);
                conditions.set(i, intersection);
                return overlapsOtherScales(conditions, intersection);
            }
        }
        // 同一属性使用不同 scale 时保留各自的区间，在缩放前的属性值上检查它们能否同时成立。
        if (next instanceof ItemBindingCondition.NumericRange range && !overlapsOtherScales(conditions, range)) {
            return false;
        }
        conditions.add(next);
        return true;
    }

    private static boolean overlapsOtherScales(List<ItemBindingCondition> conditions, ItemBindingCondition.NumericRange next) {
        for (ItemBindingCondition condition : conditions) {
            if (condition instanceof ItemBindingCondition.NumericRange range && range.property().equals(next.property())
                    && range.scale() != 0 && next.scale() != 0 && range.scale() != next.scale()
                    && (lowerBound(next) > upperBound(range) || lowerBound(range) > upperBound(next))) {
                return false;
            }
        }
        return true;
    }

    private static double lowerBound(ItemBindingCondition.NumericRange range) {
        if (range.scale() > 0) {
            return range.minInclusive() == null ? Double.NEGATIVE_INFINITY : thresholdInput(range.minInclusive(), range.scale());
        }
        return range.maxExclusive() == null ? Double.NEGATIVE_INFINITY : -Math.nextDown(thresholdInput(range.maxExclusive(), -range.scale()));
    }

    private static double upperBound(ItemBindingCondition.NumericRange range) {
        if (range.scale() > 0) {
            return range.maxExclusive() == null ? Double.POSITIVE_INFINITY : Math.nextDown(thresholdInput(range.maxExclusive(), range.scale()));
        }
        return range.minInclusive() == null ? Double.POSITIVE_INFINITY : -thresholdInput(range.minInclusive(), -range.scale());
    }

    private static float thresholdInput(float threshold, float positiveScale) {
        // Geyser 以 float 乘法判断阈值。直接除以 scale 会在临界点丢失舍入信息。
        float low = -Float.MAX_VALUE;
        float high = Float.MAX_VALUE;
        if (low * positiveScale >= threshold) {
            return low;
        }
        if (high * positiveScale < threshold) {
            return Float.POSITIVE_INFINITY;
        }
        while (Math.nextUp(low) < high) {
            float middle = (float) (((double) low + high) / 2);
            if (middle * positiveScale >= threshold) {
                high = middle;
            } else {
                low = middle;
            }
        }
        return high;
    }

    private static ItemBindingCondition.SelectMatch intersectSelect(ItemBindingCondition.SelectMatch left, ItemBindingCondition.SelectMatch right) {
        List<JsonElement> values = new ArrayList<>();
        if (left.negated() && right.negated()) {
            values.addAll(left.values());
            for (JsonElement value : right.values()) {
                if (!values.contains(value)) {
                    values.add(value);
                }
            }
            return new ItemBindingCondition.SelectMatch(left.property(), values, true);
        }
        ItemBindingCondition.SelectMatch allowed = left.negated() ? right : left;
        ItemBindingCondition.SelectMatch other = left.negated() ? left : right;
        for (JsonElement value : allowed.values()) {
            if (other.matches(value)) {
                values.add(value);
            }
        }
        return new ItemBindingCondition.SelectMatch(left.property(), values, false);
    }
}
