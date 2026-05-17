package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class Equipments {
    public static final EquipmentType<TrimBasedEquipment> TRIM = register(Key.ce("trim"), TrimBasedEquipment.FACTORY);
    public static final EquipmentType<ComponentBasedEquipment> COMPONENT = register(Key.ce("component"), ComponentBasedEquipment.FACTORY);

    private Equipments() {}

    public static <E extends Equipment> EquipmentType<E> register(Key key, EquipmentFactory<E> factory) {
        EquipmentType<E> type = new EquipmentType<>(key, factory);
        ((WritableRegistry<EquipmentType<?>>) BuiltInRegistries.EQUIPMENT_TYPE)
                .register(ResourceKey.create(Registries.EQUIPMENT_TYPE.location(), key), type);
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Equipment> E fromConfig(Key id, ConfigSection section) {
        String typeName = section.getNonEmptyString("type");
        Key key = Key.ce(typeName);
        EquipmentType<E> equipmentType = (EquipmentType<E>) BuiltInRegistries.EQUIPMENT_TYPE.getValue(key);
        if (equipmentType == null) {
            throw new KnownResourceException("resource.equipment.unknown_type", section.assemblePath("type"), key.asString());
        }
        return equipmentType.factory().create(id, section);
    }
}
