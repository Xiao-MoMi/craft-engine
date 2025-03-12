package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class CustomModelDataConditionProperty implements ConditionProperty {
    public static final Factory FACTORY = new Factory();
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
        if (index != 0)
            jsonObject.addProperty("index", index);
    }

    public static class Factory implements ConditionPropertyFactory {

        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            int index = MiscUtils.getAsInt(arguments.getOrDefault("index", 0));
            return new CustomModelDataConditionProperty(index);
        }
    }
}
