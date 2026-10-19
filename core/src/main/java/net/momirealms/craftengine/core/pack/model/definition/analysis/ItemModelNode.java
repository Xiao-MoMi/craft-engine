package net.momirealms.craftengine.core.pack.model.definition.analysis;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.SpecialItemModel;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.RangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.SelectProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public sealed interface ItemModelNode {

    String path();

    @Nullable
    default Transformation transformation() {
        return null;
    }

    record Model(String path, BaseItemModel definition) implements ItemModelNode {

        @Override
        public Transformation transformation() {
            return this.definition.transformation();
        }
    }

    record Special(String path, SpecialItemModel definition) implements ItemModelNode {

        @Override
        public Transformation transformation() {
            return this.definition.transformation();
        }
    }

    record Empty(String path) implements ItemModelNode {
    }

    record Missing(String path) implements ItemModelNode {
    }

    record BundleSelected(String path) implements ItemModelNode {
    }

    record Composite(String path, List<ItemModelNode> children, Transformation transformation) implements ItemModelNode {

        public Composite {
            children = List.copyOf(children);
        }
    }

    record Condition(String path, ConditionProperty property, ItemModelNode onTrue, ItemModelNode onFalse,
                     Transformation transformation) implements ItemModelNode {
    }

    record Select(String path, SelectProperty property, Map<JsonElement, ItemModelNode> cases, ItemModelNode fallback,
                  Transformation transformation) implements ItemModelNode {

        public Select {
            cases = Collections.unmodifiableMap(new LinkedHashMap<>(cases));
        }
    }

    record Range(String path, RangeDispatchProperty property, float scale, List<Entry> entries, ItemModelNode fallback,
                 Transformation transformation) implements ItemModelNode {

        public Range {
            entries = List.copyOf(entries);
        }

        public ItemModelNode select(float propertyValue) {
            float value = propertyValue * this.scale;
            if (Float.isNaN(value)) {
                return this.fallback;
            }
            // Java 26.3 uses linear search below 16 entries and Arrays.binarySearch otherwise.
            int index = -1;
            if (this.entries.size() < 16) {
                for (int i = 0; i < this.entries.size(); i++) {
                    if (!(this.entries.get(i).threshold() <= value)) {
                        break;
                    }
                    index = i;
                }
            } else {
                int low = 0;
                int high = this.entries.size() - 1;
                while (low <= high) {
                    int middle = (low + high) >>> 1;
                    int comparison = Float.compare(this.entries.get(middle).threshold(), value);
                    if (comparison < 0) {
                        low = middle + 1;
                    } else if (comparison > 0) {
                        high = middle - 1;
                    } else {
                        return this.entries.get(middle).model();
                    }
                }
                index = low - 1;
            }
            return index < 0 ? this.fallback : this.entries.get(index).model();
        }
    }

    record Entry(float threshold, ItemModelNode model) {
    }
}
