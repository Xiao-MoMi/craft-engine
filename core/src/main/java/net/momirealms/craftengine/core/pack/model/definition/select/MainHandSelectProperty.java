package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class MainHandSelectProperty implements SelectProperty, LegacyModelPredicate<String> {
    public static final SelectPropertyFactory<MainHandSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<MainHandSelectProperty> READER = new Reader();
    public static final MainHandSelectProperty INSTANCE = new MainHandSelectProperty();

    private MainHandSelectProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "main_hand");
    }

    @Override
    public String legacyPredicateId(Key material) {
        return "lefthanded";
    }

    @Override
    public Number toLegacyValue(String value) {
        if (value.equals("left")) return 1;
        return 0;
    }

    private static class Factory implements SelectPropertyFactory<MainHandSelectProperty> {
        @Override
        public MainHandSelectProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements SelectPropertyReader<MainHandSelectProperty> {
        @Override
        public MainHandSelectProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
