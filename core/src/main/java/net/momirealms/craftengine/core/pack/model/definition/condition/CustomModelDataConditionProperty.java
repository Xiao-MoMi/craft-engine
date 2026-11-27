package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public final class CustomModelDataConditionProperty implements ConditionProperty {
    public static final ConditionPropertyFactory<CustomModelDataConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<CustomModelDataConditionProperty> READER = new Reader();
    private final int index;

    public CustomModelDataConditionProperty(int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "custom_model_data");
        if (this.index != 0) {
            model.addProperty("index", this.index);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CustomModelDataConditionProperty that)) {
            return false;
        }
        return this.index == that.index;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.index);
    }

    private static class Factory implements ConditionPropertyFactory<CustomModelDataConditionProperty> {
        @Override
        public CustomModelDataConditionProperty create(ConfigSection section) {
            return new CustomModelDataConditionProperty(section.getInt("index"));
        }
    }

    private static class Reader implements ConditionPropertyReader<CustomModelDataConditionProperty> {
        @Override
        public CustomModelDataConditionProperty read(JsonObject json) {
            return new CustomModelDataConditionProperty(json.has("index") ? json.get("index").getAsInt() : 0);
        }
    }
}
