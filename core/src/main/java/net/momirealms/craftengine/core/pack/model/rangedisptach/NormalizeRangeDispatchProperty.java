package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class NormalizeRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final Key type;
    private final boolean normalize;

    public NormalizeRangeDispatchProperty(Key type, boolean normalize) {
        this.type = type;
        this.normalize = normalize;
    }

    @Override
    public Key type() {
        return type;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        if (!normalize) {
            jsonObject.addProperty("normalize", false);
        }
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            Key type = Key.of(arguments.get("property").toString());
            boolean normalize = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("normalize", true), "normalize");
            return new NormalizeRangeDispatchProperty(type, normalize);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            Key type = Key.of(json.get("property").toString());
            boolean normalize = !json.has("normalize") || json.get("normalize").getAsBoolean();
            return new NormalizeRangeDispatchProperty(type, normalize);
        }
    }
}
