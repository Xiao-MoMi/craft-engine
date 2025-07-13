package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class Tints {
    public static final Key CONSTANT = Key.of("minecraft:constant");
    public static final Key CUSTOM_MODEL_DATA = Key.of("minecraft:custom_model_data");
    public static final Key DYE = Key.of("minecraft:dye");
    public static final Key FIREWORK = Key.of("minecraft:firework");
    public static final Key GRASS = Key.of("minecraft:grass");
    public static final Key MAP_COLOR = Key.of("minecraft:map_color");
    public static final Key POTION = Key.of("minecraft:potion");
    public static final Key TEAM = Key.of("minecraft:team");

    static {
        registerFactory(CONSTANT, ConstantTint.FACTORY);
        registerReader(CONSTANT, ConstantTint.READER);
        registerFactory(CUSTOM_MODEL_DATA, CustomModelDataTint.FACTORY);
        registerReader(CUSTOM_MODEL_DATA, CustomModelDataTint.READER);
        registerFactory(GRASS, GrassTint.FACTORY);
        registerReader(GRASS, GrassTint.READER);
        registerFactory(DYE, SimpleDefaultTint.FACTORY);
        registerReader(DYE, SimpleDefaultTint.READER);
        registerFactory(FIREWORK, SimpleDefaultTint.FACTORY);
        registerReader(FIREWORK, SimpleDefaultTint.READER);
        registerFactory(MAP_COLOR, SimpleDefaultTint.FACTORY);
        registerReader(MAP_COLOR, SimpleDefaultTint.READER);
        registerFactory(POTION, SimpleDefaultTint.FACTORY);
        registerReader(POTION, SimpleDefaultTint.READER);
        registerFactory(TEAM, SimpleDefaultTint.FACTORY);
        registerReader(TEAM, SimpleDefaultTint.READER);
    }

    public static void registerFactory(Key key, TintFactory factory) {
        ((WritableRegistry<TintFactory>) BuiltInRegistries.TINT_FACTORY)
                .register(ResourceKey.create(Registries.TINT_FACTORY.location(), key), factory);
    }

    public static void registerReader(Key key, TintReader reader) {
        ((WritableRegistry<TintReader>) BuiltInRegistries.TINT_READER)
                .register(ResourceKey.create(Registries.TINT_READER.location(), key), reader);
    }

    public static Tint fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.item.model.tint.missing_type");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        TintFactory factory = BuiltInRegistries.TINT_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.model.tint.invalid_type", type);
        }
        return factory.create(map);
    }

    public static Tint fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        TintReader reader = BuiltInRegistries.TINT_READER.getValue(key);
        if (reader == null) {
            throw new IllegalArgumentException("Invalid tint type: " + type);
        }
        return reader.read(json);
    }
}
