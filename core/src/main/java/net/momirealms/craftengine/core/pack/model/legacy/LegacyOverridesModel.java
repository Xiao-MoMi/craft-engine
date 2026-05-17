package net.momirealms.craftengine.core.pack.model.legacy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class LegacyOverridesModel implements Comparable<LegacyOverridesModel> {
    private final Map<String, Object> predicate;
    private final Key model;
    private final int customModelData;
    private final ModelGeneration generation;

    public LegacyOverridesModel(@Nullable Map<String, Object> predicate, @NotNull Key model, int customModelData, ModelGeneration generation) {
        this.predicate = predicate == null ? new HashMap<>() : predicate;
        this.model = model;
        this.customModelData = customModelData;
        if (customModelData > 0 && !this.predicate.containsKey("custom_model_data")) {
            this.predicate.put("custom_model_data", customModelData);
        }
        this.generation = generation;
    }

    public LegacyOverridesModel(JsonObject json) {
        this.generation = null;
        this.model = Key.of(json.get("model").getAsString());
        JsonObject predicate = json.getAsJsonObject("predicate");
        if (predicate != null) {
            this.predicate = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : predicate.entrySet()) {
                JsonElement value = entry.getValue();
                if (value instanceof JsonPrimitive primitive) {
                    if (primitive.isBoolean()) {
                        this.predicate.put(entry.getKey(), primitive.getAsBoolean());
                    } else if (primitive.isNumber()) {
                        this.predicate.put(entry.getKey(), primitive.getAsNumber());
                    } else if (primitive.isString()) {
                        this.predicate.put(entry.getKey(), primitive.getAsString());
                    }
                }
            }
            if (this.predicate.containsKey("custom_model_data")) {
                this.customModelData = Integer.parseInt(this.predicate.get("custom_model_data").toString());
            } else {
                this.customModelData = 0;
            }
        } else {
            this.predicate = Map.of();
            this.customModelData = 0;
        }
    }

    public Map<String, Object> predicate() {
        return this.predicate;
    }

    public boolean hasPredicate() {
        return this.predicate != null && !this.predicate.isEmpty();
    }

    public Key model() {
        return this.model;
    }

    public JsonObject toLegacyPredicateElement() {
        JsonObject json = new JsonObject();
        JsonObject predicateJson = new JsonObject();
        if (this.predicate != null && !this.predicate.isEmpty()) {
            for (Map.Entry<String, Object> entry : this.predicate.entrySet()) {
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
        json.addProperty("model", this.model.asMinimalString());
        return json;
    }

    public int customModelData() {
        return this.customModelData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegacyOverridesModel that = (LegacyOverridesModel) o;
        return this.customModelData == that.customModelData && Objects.equals(this.predicate, that.predicate) && Objects.equals(this.model, that.model);
    }

    @Override
    public int hashCode() {
        int result = this.predicate.hashCode();
        result = 31 * result + Objects.hashCode(model);
        result = 31 * result + this.customModelData;
        return result;
    }

    @Override
    public String toString() {
        return "LegacyOverridesModel{" +
                "predicate=" + this.predicate +
                ", model='" + this.model + '\'' +
                ", custom-model-data=" + this.customModelData +
                '}';
    }

    @Override
    public int compareTo(@NotNull LegacyOverridesModel o) {
        if (this.customModelData != o.customModelData) {
            return this.customModelData - o.customModelData;
        } else {
            if (this.predicate.size() != o.predicate.size()) {
                return this.predicate.size() - o.predicate.size();
            }
            for (Map.Entry<String, Object> entry : this.predicate.entrySet()) {
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

    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        if (this.generation != null) {
            consumer.accept(new ModelGenerationHolder(this.model, this.generation));
        }
    }
}
