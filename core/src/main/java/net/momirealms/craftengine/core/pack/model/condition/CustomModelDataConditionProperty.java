package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class CustomModelDataConditionProperty implements ConditionProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final int index;

    public CustomModelDataConditionProperty(int index) {
        this.index = index;
    }

    @Override
    public Key type() {
        return ConditionProperties.CUSTOM_MODEL_DATA;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        if (this.index != 0)
            jsonObject.addProperty("index", this.index);
    }

    public static class Factory implements ConditionPropertyFactory {
        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            int index = ResourceConfigUtils.getAsInt(arguments.getOrDefault("index", 0), "index");
            return new CustomModelDataConditionProperty(index);
        }
    }

    public static class Reader implements ConditionPropertyReader {
        @Override
        public ConditionProperty read(JsonObject json) {
            int index = json.has("index") ? json.get("index").getAsInt() : 0;
            return new CustomModelDataConditionProperty(index);
        }
    }
}
