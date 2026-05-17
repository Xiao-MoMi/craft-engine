package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public interface EquipmentFactory<E extends Equipment> {

    E create(Key id, ConfigSection section);
}
