package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface ItemBindingCondition {

    record BooleanMatch(ConditionProperty property, boolean expected) implements ItemBindingCondition {
    }

    record SelectMatch(SelectProperty property, List<JsonElement> values, boolean negated) implements ItemBindingCondition {

        public SelectMatch {
            values = List.copyOf(values);
        }

        public boolean matches(JsonElement value) {
            return this.values.contains(value) != this.negated;
        }
    }

    record NumericRange(RangeDispatchProperty property,
                        float scale,
                        @Nullable Float minInclusive,
                        @Nullable Float maxExclusive) implements ItemBindingCondition {

        public boolean matches(float value) {
            float scaled = value * this.scale;
            return (this.minInclusive == null || scaled >= this.minInclusive) && (this.maxExclusive == null || !(scaled >= this.maxExclusive));
        }
    }
}
