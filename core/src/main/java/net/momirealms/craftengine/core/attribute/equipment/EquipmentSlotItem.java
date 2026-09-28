package net.momirealms.craftengine.core.attribute.equipment;

import net.momirealms.craftengine.core.attribute.AttributeInstance;
import net.momirealms.craftengine.core.attribute.EntityAttributes;
import net.momirealms.craftengine.core.attribute.modifier.AttributeModifierScope;
import net.momirealms.craftengine.core.attribute.modifier.SlotAttributeModifierConfig;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.equipment.SetPotionEffect;
import net.momirealms.craftengine.core.item.setting.value.EquipmentPotionEffects;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentSlotItem {
    private final Item item;
    private final List<SlotAttributeModifierConfig> snapshots;
    private final List<SetPotionEffect> potionEffects;

    private EquipmentSlotItem(Item item, List<SlotAttributeModifierConfig> snapshots, List<SetPotionEffect> potionEffects) {
        this.item = item;
        this.snapshots = snapshots;
        this.potionEffects = potionEffects;
    }

    public static EquipmentSlotItem create(EquipmentSetSlot slot, Item item) {
        List<SlotAttributeModifierConfig> modifiers = CraftEngine.instance().attributeManager().getItemAttributeModifiers(item);
        List<SlotAttributeModifierConfig> snapshots = new ArrayList<>(modifiers.size());
        for (SlotAttributeModifierConfig config : modifiers) {
            if (config.slot.test(slot)) {
                snapshots.add(config);
            }
        }
        ItemDefinition definition = item.getDefinitionOrNull();
        EquipmentPotionEffects effects = definition != null ? definition.settings().equipmentPotionEffects() : null;
        List<SetPotionEffect> potionEffects = effects != null ? effects.effects(slot) : List.of();
        return new EquipmentSlotItem(
                item,
                snapshots.isEmpty() ? List.of() : List.copyOf(snapshots),
                potionEffects
        );
    }

    public Item item() {
        return this.item;
    }

    public List<SlotAttributeModifierConfig> snapshots() {
        return this.snapshots;
    }

    public List<SetPotionEffect> potionEffects() {
        return this.potionEffects;
    }

    public void addOrUpdateModifiers(EntityAttributes attributes) {
        for (SlotAttributeModifierConfig config : this.snapshots) {
            if (config.scope == AttributeModifierScope.WEAPON) continue;
            AttributeInstance instance = attributes.getInstance(config.attribute);
            if (instance == null) continue;
            instance.addOrUpdateModifier(config.build(this.item));
        }
    }

    public void removeModifiers(EntityAttributes attributes) {
        for (SlotAttributeModifierConfig config : this.snapshots) {
            if (config.scope == AttributeModifierScope.WEAPON) continue;
            AttributeInstance instance = attributes.getInstance(config.attribute);
            if (instance == null) continue;
            instance.removeModifier(config.id);
        }
    }
}
