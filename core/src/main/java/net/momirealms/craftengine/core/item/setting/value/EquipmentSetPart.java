package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.attribute.equipment.EquipmentSetComponent;
import net.momirealms.craftengine.core.attribute.equipment.EquipmentSetSlot;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentSetPart {
    private final EquipmentSetComponent[] components;

    public EquipmentSetPart(List<EquipmentSetComponent> components) {
        this.components = components.toArray(new EquipmentSetComponent[0]);
    }

    public List<Key> getMatchingSets(EquipmentSetSlot slot) {
        List<Key> sets = new ArrayList<>(1);
        for (EquipmentSetComponent component : this.components) {
            if (component.slots().contains(slot)) {
                sets.addAll(component.sets());
            }
        }
        return sets;
    }
}
