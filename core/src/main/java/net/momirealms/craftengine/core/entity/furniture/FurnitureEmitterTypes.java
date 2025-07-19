package net.momirealms.craftengine.core.entity.furniture;

import java.util.Map;
import java.util.Optional;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public class FurnitureEmitterTypes {
    public static final Key PARTICLE = Key.of("minecraft:particle");
    
    static {
        register(PARTICLE, new ParticleEmitterFactory());
    }

    public static void register(Key key, FurnitureEmitterFactory factory) {
        Holder.Reference<FurnitureEmitterFactory> holder = ((WritableRegistry<FurnitureEmitterFactory>) BuiltInRegistries.EMITTER_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.EMITTER_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static FurnitureEmitter fromMap(Map<String, Object> arguments) {
        Key type = Optional.ofNullable(arguments.get("type")).map(String::valueOf).map(Key::of).orElse(FurnitureEmitterTypes.PARTICLE);
        FurnitureEmitterFactory factory = BuiltInRegistries.EMITTER_FACTORY.getValue(type);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.furniture.emitter.invalid_type", type.toString());
        }
        return factory.create(arguments);
    }
}
