package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class NormalizeRangeDispatchProperty implements RangeDispatchProperty {
    public static final RangeDispatchPropertyFactory<NormalizeRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<NormalizeRangeDispatchProperty> READER = new Reader();
    private final Key type;
    private final boolean normalize;

    public NormalizeRangeDispatchProperty(Key type, boolean normalize) {
        this.type = type;
        this.normalize = normalize;
    }

    public Key type() {
        return this.type;
    }

    public boolean normalize() {
        return this.normalize;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", this.type.asMinimalString());
        if (!this.normalize) {
            model.addProperty("normalize", false);
        }
    }

    private static class Factory implements RangeDispatchPropertyFactory<NormalizeRangeDispatchProperty> {
        @Override
        public NormalizeRangeDispatchProperty create(ConfigSection section) {
            return new NormalizeRangeDispatchProperty(
                    section.getNonNullIdentifier("property"),
                    section.getBoolean("normalize", true)
            );
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<NormalizeRangeDispatchProperty> {
        @Override
        public NormalizeRangeDispatchProperty read(JsonObject json) {
            return new NormalizeRangeDispatchProperty(
                    Key.of(json.get("property").toString()),
                    !json.has("normalize") || json.get("normalize").getAsBoolean()
            );
        }
    }
}
