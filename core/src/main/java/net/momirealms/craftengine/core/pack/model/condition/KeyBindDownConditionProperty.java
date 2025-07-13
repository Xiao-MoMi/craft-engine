package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class KeyBindDownConditionProperty implements ConditionProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String keybind;

    public KeyBindDownConditionProperty(String keybind) {
        this.keybind = keybind;
    }

    @Override
    public Key type() {
        return ConditionProperties.KEYBIND_DOWN;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("keybind", keybind);
    }

    public static class Factory implements ConditionPropertyFactory {
        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            String keybindObj = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("keybind"), "warning.config.item.model.condition.keybind.missing_keybind");
            return new KeyBindDownConditionProperty(keybindObj);
        }
    }

    public static class Reader implements ConditionPropertyReader {
        @Override
        public ConditionProperty read(JsonObject json) {
            String keybind = json.get("keybind").getAsString();
            return new KeyBindDownConditionProperty(keybind);
        }
    }
}
