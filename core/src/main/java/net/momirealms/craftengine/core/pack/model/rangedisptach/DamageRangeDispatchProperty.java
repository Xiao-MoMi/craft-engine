package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class DamageRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final boolean normalize;

    public DamageRangeDispatchProperty(boolean normalize) {
        this.normalize = normalize;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.DAMAGE;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        if (!normalize) {
            jsonObject.addProperty("normalize", false);
        }
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (this.normalize) return "damage";
        throw new RuntimeException("Enable 'normalize' option if you want to use 'damage' on 1.21.3 and below");
    }

    @Override
    public Number toLegacyValue(Number value) {
        if (this.normalize) return value;
        throw new RuntimeException("Enable 'normalize' option if you want to use 'damage' on 1.21.3 and below");
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            boolean normalize = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("normalize", true), "normalize");
            return new DamageRangeDispatchProperty(normalize);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            boolean normalize = !json.has("normalize") || json.get("normalize").getAsBoolean();
            return new DamageRangeDispatchProperty(normalize);
        }
    }
}
