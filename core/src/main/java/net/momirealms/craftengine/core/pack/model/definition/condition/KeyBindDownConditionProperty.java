package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import java.util.Objects;

public final class KeyBindDownConditionProperty implements ConditionProperty {
    public static final ConditionPropertyFactory<KeyBindDownConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<KeyBindDownConditionProperty> READER = new Reader();
    private final String keybind;

    public KeyBindDownConditionProperty(String keybind) {
        this.keybind = keybind;
    }

    public String keybind() {
        return this.keybind;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "keybind_down");
        model.addProperty("keybind", this.keybind);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof KeyBindDownConditionProperty that)) {
            return false;
        }
        return Objects.equals(this.keybind, that.keybind);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.keybind);
    }

    private static class Factory implements ConditionPropertyFactory<KeyBindDownConditionProperty> {
        @Override
        public KeyBindDownConditionProperty create(ConfigSection section) {
            return new KeyBindDownConditionProperty(section.getNonNullString("keybind"));
        }
    }

    private static class Reader implements ConditionPropertyReader<KeyBindDownConditionProperty> {
        @Override
        public KeyBindDownConditionProperty read(JsonObject json) {
            return new KeyBindDownConditionProperty(json.get("keybind").getAsString());
        }
    }
}
