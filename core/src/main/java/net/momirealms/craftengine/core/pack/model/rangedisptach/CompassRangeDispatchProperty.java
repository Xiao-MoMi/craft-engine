package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;

public class CompassRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    private final String target;

    public CompassRangeDispatchProperty(String target) {
        this.target = target;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.COMPASS;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("target", target);
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            String target = Objects.requireNonNull(arguments.get("target")).toString();
            return new CompassRangeDispatchProperty(target);
        }
    }
}
