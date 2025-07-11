package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class RangeDispatchProperties {
    public static final Key BUNDLE_FULLNESS = Key.of("minecraft:bundle/fullness");
    public static final Key COMPASS = Key.of("minecraft:compass");
    public static final Key COOLDOWN = Key.of("minecraft:cooldown");
    public static final Key COUNT = Key.of("minecraft:count");
    public static final Key CROSSBOW_PULL = Key.of("minecraft:crossbow/pull");
    public static final Key CUSTOM_MODEL_DATA = Key.of("minecraft:custom_model_data");
    public static final Key DAMAGE = Key.of("minecraft:damage");
    public static final Key TIME = Key.of("minecraft:time");
    public static final Key USE_CYCLE = Key.of("minecraft:use_cycle");
    public static final Key USE_DURATION = Key.of("minecraft:use_duration");

    static {
        registerFactory(BUNDLE_FULLNESS, SimpleRangeDispatchProperty.FACTORY);
        registerReader(BUNDLE_FULLNESS, SimpleRangeDispatchProperty.READER);
        registerFactory(COOLDOWN, SimpleRangeDispatchProperty.FACTORY);
        registerReader(COOLDOWN, SimpleRangeDispatchProperty.READER);
        registerFactory(CROSSBOW_PULL, CrossBowPullingRangeDispatchProperty.FACTORY);
        registerReader(CROSSBOW_PULL, CrossBowPullingRangeDispatchProperty.READER);
        registerFactory(COMPASS, CompassRangeDispatchProperty.FACTORY);
        registerReader(COMPASS, CompassRangeDispatchProperty.READER);
        registerFactory(COUNT, NormalizeRangeDispatchProperty.FACTORY);
        registerReader(COUNT, NormalizeRangeDispatchProperty.READER);
        registerFactory(DAMAGE, DamageRangeDispatchProperty.FACTORY);
        registerReader(DAMAGE, DamageRangeDispatchProperty.READER);
        registerFactory(CUSTOM_MODEL_DATA, CustomModelDataRangeDispatchProperty.FACTORY);
        registerReader(CUSTOM_MODEL_DATA, CustomModelDataRangeDispatchProperty.READER);
        registerFactory(TIME, TimeRangeDispatchProperty.FACTORY);
        registerReader(TIME, TimeRangeDispatchProperty.READER);
        registerFactory(USE_CYCLE, UseCycleRangeDispatchProperty.FACTORY);
        registerReader(USE_CYCLE, UseCycleRangeDispatchProperty.READER);
        registerFactory(USE_DURATION, UseDurationRangeDispatchProperty.FACTORY);
        registerReader(USE_DURATION, UseDurationRangeDispatchProperty.READER);
    }

    public static void registerFactory(Key key, RangeDispatchPropertyFactory factory) {
        ((WritableRegistry<RangeDispatchPropertyFactory>) BuiltInRegistries.RANGE_DISPATCH_PROPERTY_FACTORY)
                .register(ResourceKey.create(Registries.RANGE_DISPATCH_PROPERTY_FACTORY.location(), key), factory);
    }

    public static void registerReader(Key key, RangeDispatchPropertyReader reader) {
        ((WritableRegistry<RangeDispatchPropertyReader>) BuiltInRegistries.RANGE_DISPATCH_PROPERTY_READER)
                .register(ResourceKey.create(Registries.RANGE_DISPATCH_PROPERTY_READER.location(), key), reader);
    }

    public static RangeDispatchProperty fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("property"), "warning.config.item.model.range_dispatch.missing_property");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        RangeDispatchPropertyFactory factory = BuiltInRegistries.RANGE_DISPATCH_PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.model.range_dispatch.invalid_property", type);
        }
        return factory.create(map);
    }

    public static RangeDispatchProperty fromJson(JsonObject json) {
        String type = json.get("property").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        RangeDispatchPropertyReader reader = BuiltInRegistries.RANGE_DISPATCH_PROPERTY_READER.getValue(key);
        if (reader == null) {
            throw new IllegalArgumentException("Invalid range dispatch property type: " + key);
        }
        return reader.read(json);
    }
}
