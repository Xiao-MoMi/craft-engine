package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.Transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record ItemBindingRule(List<ItemBindingCondition> conditions,  // 到达此分支的全部条件链
                              ItemRenderTarget target,  // 根模型类型，special或者model那些
                              List<Transformation> transformations,
                              String sourcePath) {

    public ItemBindingRule {
        // 必须复制，因为这个list会变
        conditions = List.copyOf(conditions);
        transformations = List.copyOf(transformations);
    }

    boolean hasCondition(Predicate<ItemBindingCondition> predicate) {
        for (ItemBindingCondition condition : this.conditions) {
            if (predicate.test(condition)) {
                return true;
            }
        }
        return false;
    }

    List<ItemBindingCondition> conditionsExcept(Predicate<ItemBindingCondition> excluded) {
        int firstExcluded = 0;
        while (firstExcluded < this.conditions.size() && !excluded.test(this.conditions.get(firstExcluded))) {
            firstExcluded++;
        }
        if (firstExcluded == this.conditions.size()) {
            return this.conditions;
        }
        List<ItemBindingCondition> kept = new ArrayList<>(firstExcluded);
        for (int i = 0; i < firstExcluded; i++) {
            kept.add(this.conditions.get(i));
        }
        for (int i = firstExcluded + 1; i < this.conditions.size(); i++) {
            ItemBindingCondition condition = this.conditions.get(i);
            if (!excluded.test(condition)) {
                kept.add(condition);
            }
        }
        return List.copyOf(kept);
    }

    boolean matchesConditionsExcept(List<ItemBindingCondition> expected, Predicate<ItemBindingCondition> excluded) {
        int index = 0;
        for (ItemBindingCondition condition : this.conditions) {
            if (excluded.test(condition)) {
                continue;
            }
            if (index == expected.size() || !condition.equals(expected.get(index))) {
                return false;
            }
            index++;
        }
        return index == expected.size();
    }
}
