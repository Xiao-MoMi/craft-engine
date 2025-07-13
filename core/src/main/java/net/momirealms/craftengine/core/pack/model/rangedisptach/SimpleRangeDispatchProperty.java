package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class SimpleRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final Key type;

    public SimpleRangeDispatchProperty(Key type) {
        this.type = type;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type.toString());
    }

    @Override
    public Key type() {
        return type;
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            Key type = Key.of(arguments.get("property").toString());
            return new SimpleRangeDispatchProperty(type);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            Key type = Key.of(json.get("property").getAsString());
            return new SimpleRangeDispatchProperty(type);
        }
    }
}
