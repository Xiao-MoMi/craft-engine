package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;

public class HasComponentConditionProperty implements ConditionProperty {
    public static final Factory FACTORY = new Factory();
    private final String component;
    private final boolean ignoreDefault;

    public HasComponentConditionProperty(String component, boolean ignoreDefault) {
        this.component = component;
        this.ignoreDefault = ignoreDefault;
    }

    @Override
    public Key type() {
        return ConditionProperties.HAS_COMPONENT;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("component", component);
        if (ignoreDefault) {
            jsonObject.addProperty("ignore_default", true);
        }
    }

    public static class Factory implements ConditionPropertyFactory {

        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            boolean ignoreDefault = (boolean) arguments.getOrDefault("ignore-default", false);
            String component = Objects.requireNonNull(arguments.get("component"), "component").toString();
            return new HasComponentConditionProperty(component, ignoreDefault);
        }
    }
}
