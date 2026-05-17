package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class ChargeTypeSelectProperty implements SelectProperty, LegacyModelPredicate<String> {
    public static final SelectPropertyFactory<ChargeTypeSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<ChargeTypeSelectProperty> READER = new Reader();
    public static final ChargeTypeSelectProperty INSTANCE = new ChargeTypeSelectProperty();

    private ChargeTypeSelectProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "charge_type");
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.CROSSBOW)) return "firework";
        return null;
    }

    @Override
    public Number toLegacyValue(String value) {
        if (value.equals("rocket")) return 1;
        return 0;
    }

    private static class Factory implements SelectPropertyFactory<ChargeTypeSelectProperty> {
        @Override
        public ChargeTypeSelectProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements SelectPropertyReader<ChargeTypeSelectProperty> {
        @Override
        public ChargeTypeSelectProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
