package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import org.jetbrains.annotations.NotNull;

public final class HasComponentConditionProperty implements ConditionProperty {
    public static final ConditionPropertyFactory<HasComponentConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<HasComponentConditionProperty> READER = new Reader();
    private final String component;
    private final boolean ignoreDefault;

    public HasComponentConditionProperty(@NotNull String component, boolean ignoreDefault) {
        this.component = component;
        this.ignoreDefault = ignoreDefault;
    }

    @NotNull
    public String component() {
        return this.component;
    }

    public boolean ignoreDefault() {
        return this.ignoreDefault;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "has_component");
        model.addProperty("component", this.component);
        if (this.ignoreDefault) {
            model.addProperty("ignore_default", true);
        }
    }

    private static class Factory implements ConditionPropertyFactory<HasComponentConditionProperty> {
        private static final String[] IGNORE_DEFAULT = new String[]{"ignore_default", "ignore-default"};

        @Override
        public HasComponentConditionProperty create(ConfigSection section) {
            return new HasComponentConditionProperty(
                    section.getNonNullString("component"),
                    section.getBoolean(IGNORE_DEFAULT)
            );
        }
    }

    private static class Reader implements ConditionPropertyReader<HasComponentConditionProperty> {
        @Override
        public HasComponentConditionProperty read(JsonObject json) {
            return new HasComponentConditionProperty(
                    json.get("component").getAsString(),
                    json.has("ignore_default") && json.get("ignore_default").getAsBoolean()
            );
        }
    }
}
