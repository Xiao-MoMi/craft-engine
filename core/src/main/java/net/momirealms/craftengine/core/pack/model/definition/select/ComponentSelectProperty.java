package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public final class ComponentSelectProperty implements SelectProperty {
    public static final SelectPropertyFactory<ComponentSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<ComponentSelectProperty> READER = new Reader();
    private final String component;

    public ComponentSelectProperty(String component) {
        this.component = component;
    }

    public String component() {
        return this.component;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "component");
        model.addProperty("component", this.component);
    }

    private static class Factory implements SelectPropertyFactory<ComponentSelectProperty> {
        @Override
        public ComponentSelectProperty create(ConfigSection section) {
            return new ComponentSelectProperty(section.getNonNullString("component"));
        }
    }

    private static class Reader implements SelectPropertyReader<ComponentSelectProperty> {
        @Override
        public ComponentSelectProperty read(JsonObject json) {
            return new ComponentSelectProperty(json.get("component").getAsString());
        }
    }
}
