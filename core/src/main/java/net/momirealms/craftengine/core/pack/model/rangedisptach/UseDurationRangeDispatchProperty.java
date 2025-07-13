package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class UseDurationRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final boolean remaining;

    public UseDurationRangeDispatchProperty(boolean remaining) {
        this.remaining = remaining;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.USE_DURATION;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        if (remaining) {
            jsonObject.addProperty("remaining", true);
        }
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.BOW)) return "pull";
        return null;
    }

    @Override
    public Number toLegacyValue(Number value) {
        return value;
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            boolean remaining = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("remaining", false), "remaining");
            return new UseDurationRangeDispatchProperty(remaining);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            boolean remaining = json.has("remaining") && json.get("remaining").getAsBoolean();
            return new UseDurationRangeDispatchProperty(remaining);
        }
    }
}
