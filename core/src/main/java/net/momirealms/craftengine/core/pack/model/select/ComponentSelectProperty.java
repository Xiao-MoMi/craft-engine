package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class ComponentSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String component;

    public ComponentSelectProperty(String component) {
        this.component = component;
    }

    @Override
    public Key type() {
        return SelectProperties.COMPONENT;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("component", this.component);
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            String component = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("component"), "warning.config.item.model.select.component.missing_component");
            return new ComponentSelectProperty(component);
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            String component = json.get("component").getAsString();
            return new ComponentSelectProperty(component);
        }
    }
}
