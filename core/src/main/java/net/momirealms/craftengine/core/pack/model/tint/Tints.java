package net.momirealms.craftengine.core.pack.model.tint;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
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
        register(CONSTANT, ConstantTint.FACTORY);
        register(CUSTOM_MODEL_DATA, CustomModelDataTint.FACTORY);
        register(GRASS, GrassTint.FACTORY);
        register(DYE, SimpleDefaultTint.FACTORY);
        register(FIREWORK, SimpleDefaultTint.FACTORY);
        register(MAP_COLOR, SimpleDefaultTint.FACTORY);
        register(POTION, SimpleDefaultTint.FACTORY);
        register(TEAM, SimpleDefaultTint.FACTORY);
    }

    public static void register(Key key, TintFactory factory) {
        Holder.Reference<TintFactory> holder = ((WritableRegistry<TintFactory>) BuiltInRegistries.TINT_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.TINT_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static Tint fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new NullPointerException("tint type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "minecraft");
        TintFactory factory = BuiltInRegistries.TINT_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown tint type: " + type);
        }
        return factory.create(map);
    }
}
