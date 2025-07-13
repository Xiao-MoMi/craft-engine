package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;
import java.util.Optional;

public class HitBoxTypes {
    public static final Key INTERACTION = Key.of("minecraft:interaction");
    public static final Key SHULKER = Key.of("minecraft:shulker");
    public static final Key HAPPY_GHAST = Key.of("minecraft:happy_ghast");
    public static final Key CUSTOM = Key.of("minecraft:custom");

    public static void register(Key key, HitBoxFactory factory) {
        ((WritableRegistry<HitBoxFactory>) BuiltInRegistries.HITBOX_FACTORY)
                .register(ResourceKey.create(Registries.HITBOX_FACTORY.location(), key), factory);
    }

    public static HitBox fromMap(Map<String, Object> arguments) {
        Key type = Optional.ofNullable(arguments.get("type")).map(String::valueOf).map(Key::of).orElse(HitBoxTypes.INTERACTION);
        HitBoxFactory factory = BuiltInRegistries.HITBOX_FACTORY.getValue(type);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.furniture.hitbox.invalid_type", type.toString());
        }
        return factory.create(arguments);
    }
}
