package net.momirealms.craftengine.core.attribute.equipment;

import net.momirealms.craftengine.core.attribute.AttributeInstance;
import net.momirealms.craftengine.core.attribute.EntityAttributes;
import net.momirealms.craftengine.core.attribute.modifier.AttributeModifierScope;
import net.momirealms.craftengine.core.attribute.modifier.SlotAttributeModifierConfig;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.equipment.SetPotionEffect;
import net.momirealms.craftengine.core.item.setting.value.EquipmentPotionEffects;
import net.momirealms.craftengine.core.item.setting.value.EquipmentSetPart;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentSlotItem {
    private final Item item;
    private final List<SlotAttributeModifierConfig> snapshots;
    private final List<SetPotionEffect> potionEffects;
    private List<Key> matchingSets;

    private EquipmentSlotItem(Item item, List<SlotAttributeModifierConfig> snapshots, List<SetPotionEffect> potionEffects, List<Key> matchingSets) {
        this.item = item;
        this.snapshots = snapshots;
        this.potionEffects = potionEffects;
        this.matchingSets = matchingSets;
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
                potionEffects,
                matchingSets(slot, definition)
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

    List<Key> matchingSets() {
        return this.matchingSets;
    }

    void refreshMatchingSets(EquipmentSetSlot slot) {
        this.matchingSets = matchingSets(slot, this.item.getDefinitionOrNull());
    }

    private static List<Key> matchingSets(EquipmentSetSlot slot, @Nullable ItemDefinition definition) {
        EquipmentSetPart part = definition != null ? definition.settings().equipmentSetPart() : null;
        return part != null ? part.getMatchingSets(slot) : List.of();
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
