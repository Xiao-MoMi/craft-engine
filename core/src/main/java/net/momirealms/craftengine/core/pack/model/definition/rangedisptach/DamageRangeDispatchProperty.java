package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class DamageRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final RangeDispatchPropertyFactory<DamageRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<DamageRangeDispatchProperty> READER = new Reader();
    private final boolean normalize;

    public DamageRangeDispatchProperty(boolean normalize) {
        this.normalize = normalize;
    }

    public boolean normalize() {
        return this.normalize;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "damage");
        if (!normalize) {
            model.addProperty("normalize", false);
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

    private static class Factory implements RangeDispatchPropertyFactory<DamageRangeDispatchProperty> {
        @Override
        public DamageRangeDispatchProperty create(ConfigSection section) {
            return new DamageRangeDispatchProperty(section.getBoolean("normalize", true));
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<DamageRangeDispatchProperty> {
        @Override
        public DamageRangeDispatchProperty read(JsonObject json) {
            return new DamageRangeDispatchProperty(!json.has("normalize") || json.get("normalize").getAsBoolean());
        }
    }
}
