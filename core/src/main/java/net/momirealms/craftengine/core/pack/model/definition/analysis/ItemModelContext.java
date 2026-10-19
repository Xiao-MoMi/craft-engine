package net.momirealms.craftengine.core.pack.model.definition.analysis;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectProperty;

public interface ItemModelContext {

    default boolean condition(ConditionProperty property) {
        throw new UnsupportedOperationException("No Java condition evaluator for " + property);
    }

    default float number(RangeDispatchProperty property) {
        throw new UnsupportedOperationException("No Java numeric evaluator for " + property);
    }

    default JsonElement select(SelectProperty property) {
        throw new UnsupportedOperationException("No Java select evaluator for " + property);
    }
}
