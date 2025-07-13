package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class SimpleSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final Key type;

    public SimpleSelectProperty(Key type) {
        this.type = type;
    }

    @Override
    public Key type() {
        return type;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            Key type = Key.of(arguments.get("property").toString());
            return new SimpleSelectProperty(type);
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            Key type = Key.of(json.get("property").getAsString());
            return new SimpleSelectProperty(type);
        }
    }
}
