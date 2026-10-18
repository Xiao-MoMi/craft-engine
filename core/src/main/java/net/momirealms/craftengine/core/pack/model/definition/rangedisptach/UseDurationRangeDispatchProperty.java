package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class UseDurationRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final RangeDispatchPropertyFactory<UseDurationRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<UseDurationRangeDispatchProperty> READER = new Reader();
    private final boolean remaining;

    public UseDurationRangeDispatchProperty(boolean remaining) {
        this.remaining = remaining;
    }

    public boolean remaining() {
        return this.remaining;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "use_duration");
        if (this.remaining) {
            model.addProperty("remaining", true);
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UseDurationRangeDispatchProperty that)) {
            return false;
        }
        return this.remaining == that.remaining;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(this.remaining);
    }

    private static class Factory implements RangeDispatchPropertyFactory<UseDurationRangeDispatchProperty> {
        @Override
        public UseDurationRangeDispatchProperty create(ConfigSection section) {
            return new UseDurationRangeDispatchProperty(section.getBoolean("remaining"));
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<UseDurationRangeDispatchProperty> {
        @Override
        public UseDurationRangeDispatchProperty read(JsonObject json) {
            return new UseDurationRangeDispatchProperty(json.has("remaining") && json.get("remaining").getAsBoolean());
        }
    }
}
