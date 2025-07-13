package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class Equipments {
    public static final Key TRIM = Key.of("craftengine:trim");
    public static final Key COMPONENT = Key.of("craftengine:component");

    static {
        register(TRIM, TrimBasedEquipment.FACTORY);
        register(COMPONENT, ComponentBasedEquipment.FACTORY);
    }

    public static void register(Key key, EquipmentFactory factory) {
        ((WritableRegistry<EquipmentFactory>) BuiltInRegistries.EQUIPMENT_FACTORY)
                .register(ResourceKey.create(Registries.EQUIPMENT_FACTORY.location(), key), factory);
    }

    public static Equipment fromMap(Key id, Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.equipment.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        EquipmentFactory factory = BuiltInRegistries.EQUIPMENT_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.equipment.invalid_type", type);
        }
        return factory.create(id, map);
    }
}
