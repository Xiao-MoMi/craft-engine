package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;

public class TimeRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    private final String source;
    private final boolean wobble;

    public TimeRangeDispatchProperty(String source, boolean wobble) {
        this.source = source;
        this.wobble = wobble;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.TIME;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("source", source);
        if (!wobble) {
            jsonObject.addProperty("wobble", false);
        }
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            String source = Objects.requireNonNull(arguments.get("source")).toString();
            boolean wobble = (boolean) arguments.getOrDefault("wobble", true);
            return new TimeRangeDispatchProperty(source, wobble);
        }
    }
}
