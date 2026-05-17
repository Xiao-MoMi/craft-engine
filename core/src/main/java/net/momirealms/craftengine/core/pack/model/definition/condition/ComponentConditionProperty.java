package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.GsonHelper;

public final class ComponentConditionProperty implements ConditionProperty {
    public static final ConditionPropertyFactory<ComponentConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<ComponentConditionProperty> READER = new Reader();
    private final String predicate;
    private final JsonElement value;

    public ComponentConditionProperty(String predicate, JsonElement value) {
        this.predicate = predicate;
        this.value = value;
    }

    public String predicate() {
        return this.predicate;
    }

    public JsonElement value() {
        return this.value;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "component");
        model.addProperty("predicate", this.predicate);
        model.add("value", this.value);
    }

    private static class Factory implements ConditionPropertyFactory<ComponentConditionProperty> {
        @Override
        public ComponentConditionProperty create(ConfigSection section) {
            return new ComponentConditionProperty(
                    section.getNonNullString("predicate"),
                    section.getNonNullValue("value", ConfigConstants.ARGUMENT_ANY, it -> GsonHelper.get().toJsonTree(it))
            );
        }
    }

    private static class Reader implements ConditionPropertyReader<ComponentConditionProperty> {
        @Override
        public ComponentConditionProperty read(JsonObject json) {
            return new ComponentConditionProperty(json.get("predicate").getAsString(), json.get("value"));
        }
    }
}
