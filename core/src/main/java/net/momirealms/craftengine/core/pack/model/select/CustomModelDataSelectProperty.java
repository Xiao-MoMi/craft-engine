package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class CustomModelDataSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final int index;

    public CustomModelDataSelectProperty(int index) {
        this.index = index;
    }

    @Override
    public Key type() {
        return SelectProperties.CUSTOM_MODEL_DATA;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("index", index);
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            int index = ResourceConfigUtils.getAsInt(arguments.getOrDefault("index", 0), "index");
            return new CustomModelDataSelectProperty(index);
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            int index = json.has("index") ? json.get("index").getAsInt() : 0;
            return new CustomModelDataSelectProperty(index);
        }
    }
}
