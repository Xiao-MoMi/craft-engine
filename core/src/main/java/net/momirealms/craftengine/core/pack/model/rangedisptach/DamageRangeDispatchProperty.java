package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class DamageRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Float> {
    public static final Factory FACTORY = new Factory();
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
    public Number toLegacyValue(Float value) {
        if (this.normalize) return value;
        throw new RuntimeException("Enable 'normalize' option if you want to use 'damage' on 1.21.3 and below");
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            boolean normalize = (boolean) arguments.getOrDefault("normalize", true);
            return new DamageRangeDispatchProperty(normalize);
        }
    }
}
