package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public final class CustomModelDataSelectProperty implements SelectProperty {
    public static final SelectPropertyFactory<CustomModelDataSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<CustomModelDataSelectProperty> READER = new Reader();
    private final int index;

    public CustomModelDataSelectProperty(int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "custom_model_data");
        model.addProperty("index", this.index);
    }

    private static class Factory implements SelectPropertyFactory<CustomModelDataSelectProperty> {
        @Override
        public CustomModelDataSelectProperty create(ConfigSection section) {
            return new CustomModelDataSelectProperty(section.getInt("index"));
        }
    }

    private static class Reader implements SelectPropertyReader<CustomModelDataSelectProperty> {
        @Override
        public CustomModelDataSelectProperty read(JsonObject json) {
            return new CustomModelDataSelectProperty(json.has("index") ? json.get("index").getAsInt() : 0);
        }
    }
}
