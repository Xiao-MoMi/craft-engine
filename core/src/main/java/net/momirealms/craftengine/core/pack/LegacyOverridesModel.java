package net.momirealms.craftengine.core.pack;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class LegacyOverridesModel implements Comparable<LegacyOverridesModel> {
    private final Map<String, Object> predicate;
    private final String model;
    private final int customModelData;

    public LegacyOverridesModel(Map<String, Object> predicate, String model, int customModelData) {
        this.predicate = predicate;
        this.model = model;
        this.customModelData = customModelData;
        if (customModelData > 0) {
            this.predicate.put("custom_model_data", customModelData);
        }
    }

    public Map<String, Object> predicate() {
        return predicate;
    }

    public boolean hasPredicate() {
        return !predicate.isEmpty();
    }

    public String model() {
        return model;
    }

    public JsonObject toLegacyPredicateElement() {
        JsonObject json = new JsonObject();
        JsonObject predicateJson = new JsonObject();
        if (predicate != null && !predicate.isEmpty()) {
            for (Map.Entry<String, Object> entry : predicate.entrySet()) {
                if (entry.getValue() instanceof Boolean b) {
                    predicateJson.addProperty(entry.getKey(), b);
                } else if (entry.getValue() instanceof Number n) {
                    predicateJson.addProperty(entry.getKey(), n);
                } else if (entry.getValue() instanceof String s) {
                    predicateJson.addProperty(entry.getKey(), s);
                }
            }
            json.add("predicate", predicateJson);
        }
        json.addProperty("model", model);
        return json;
    }

    public int customModelData() {
        return customModelData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegacyOverridesModel that = (LegacyOverridesModel) o;
        return customModelData == that.customModelData && Objects.equals(predicate, that.predicate) && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        int result = predicate.hashCode();
        result = 31 * result + Objects.hashCode(model);
        result = 31 * result + customModelData;
        return result;
    }

    @Override
    public String toString() {
        return "LegacyOverridesModel{" +
                "predicate=" + predicate +
                ", model='" + model + '\'' +
                ", custom-model-data=" + customModelData +
                '}';
    }

    @Override
    public int compareTo(@NotNull LegacyOverridesModel o) {
        if (customModelData != o.customModelData) {
            return customModelData - o.customModelData;
        } else {
            if (predicate.size() != o.predicate.size()) {
                return predicate.size() - o.predicate.size();
            }
            for (Map.Entry<String, Object> entry : predicate.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!o.predicate.containsKey(key)) {
                    return 1;
                }
                Object otherValue = o.predicate.get(key);
                int valueComparison = compareValues(value, otherValue);
                if (valueComparison != 0) {
                    return valueComparison;
                }
            }
        }
        return 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object value1, Object value2) {
        if (value1 instanceof Comparable<?> c1 && value2 instanceof Comparable<?> c2) {
            return ((Comparable) c1).compareTo(c2);
        }
        return value1.equals(value2) ? 0 : -1;
    }
}
