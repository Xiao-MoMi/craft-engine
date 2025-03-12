package net.momirealms.craftengine.core.pack.model.rangedisptach;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
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
        register(BUNDLE_FULLNESS, SimpleRangeDispatchProperty.FACTORY);
        register(COOLDOWN, SimpleRangeDispatchProperty.FACTORY);
        register(CROSSBOW_PULL, CrossBowPullingRangeDispatchProperty.FACTORY);
        register(COMPASS, CompassRangeDispatchProperty.FACTORY);
        register(COUNT, NormalizeRangeDispatchProperty.FACTORY);
        register(DAMAGE, DamageRangeDispatchProperty.FACTORY);
        register(CUSTOM_MODEL_DATA, CustomModelDataRangeDispatchProperty.FACTORY);
        register(TIME, TimeRangeDispatchProperty.FACTORY);
        register(USE_CYCLE, UseCycleRangeDispatchProperty.FACTORY);
        register(USE_DURATION, UseDurationRangeDispatchProperty.FACTORY);
    }

    public static void register(Key key, RangeDispatchPropertyFactory factory) {
        Holder.Reference<RangeDispatchPropertyFactory> holder = ((WritableRegistry<RangeDispatchPropertyFactory>) BuiltInRegistries.RANGE_DISPATCH_PROPERTY_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.RANGE_DISPATCH_PROPERTY_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static RangeDispatchProperty fromMap(Map<String, Object> map) {
        String type = (String) map.get("property");
        if (type == null) {
            throw new NullPointerException("property type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "minecraft");
        RangeDispatchPropertyFactory factory = BuiltInRegistries.RANGE_DISPATCH_PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown property type: " + type);
        }
        return factory.create(map);
    }
}
