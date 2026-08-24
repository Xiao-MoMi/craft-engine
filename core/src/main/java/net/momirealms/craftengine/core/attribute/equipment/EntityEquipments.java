package net.momirealms.craftengine.core.attribute.equipment;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.attribute.AttributeInstance;
import net.momirealms.craftengine.core.attribute.modifier.AttributeModifierConfig;
import net.momirealms.craftengine.core.attribute.modifier.AttributeModifierScope;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.equipment.EquipmentSet;
import net.momirealms.craftengine.core.item.equipment.SetPotionEffect;
import net.momirealms.craftengine.core.item.setting.value.EquipmentSetPart;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class EntityEquipments {
    private final Map<EquipmentSetSlot, EquipmentSlotItem> equipments = new Object2ObjectOpenHashMap<>();
    private final LivingEntityHolder holder;
    private Map<Key, Integer> activeSets = Map.of();
    private boolean dirty = true;

    public EntityEquipments(LivingEntityHolder holder) {
        this.holder = holder;
    }

    @Nullable
    public EquipmentSlotItem get(EquipmentSetSlot slot) {
        return this.equipments.get(slot);
    }

    @Nullable
    public EquipmentSlotItem add(EquipmentSetSlot slot, Item item) {
        EquipmentSlotItem newItem = EquipmentSlotItem.create(slot, item);
        EquipmentSlotItem previous = this.equipments.put(slot, newItem);
        this.holder.ifAttributesExist(attr -> {
            if (previous != null) {
                previous.removeModifiers(attr);
            }
            newItem.addOrUpdateModifiers(attr);
        });
        this.dirty = true;
        return previous;
    }

    @Nullable
    public EquipmentSlotItem remove(EquipmentSetSlot slot) {
        EquipmentSlotItem removed = this.equipments.remove(slot);
        if (removed != null) {
            this.holder.ifAttributesExist(removed::removeModifiers);
        }
        this.dirty = true;
        return removed;
    }

    public void updateSets() {
        updateSets(true);
    }

    public void updateSets(boolean runTransitionActions) {
        updateSetsAndReportChange(runTransitionActions);
    }

    public boolean updateSetsAndReportChange() {
        return updateSetsAndReportChange(true);
    }

    public boolean updateSetsAndReportChange(boolean runTransitionActions) {
        if (!this.dirty) return false;
        this.dirty = false;
        Map<Key, Integer> raw = computeRawActiveSets();
        Map<Key, Integer> previous = this.activeSets;
        if (raw.equals(previous)) {
            this.activeSets = raw;
            synchronizePotionEffects();
            return false;
        }
        this.activeSets = raw;
        Set<Key> names = new HashSet<>(previous.keySet());
        names.addAll(raw.keySet());
        for (Key name : names) {
            int oldPieces = previous.getOrDefault(name, 0);
            int newPieces = raw.getOrDefault(name, 0);
            if (oldPieces == newPieces) continue;
            CraftEngine.instance().attributeManager().equipmentSet(name)
                    .ifPresent(set -> applySetChange(set, oldPieces, newPieces, runTransitionActions));
        }
        synchronizePotionEffects();
        return true;
    }

    public int countSetPieces(Key set) {
        return this.activeSets.getOrDefault(set, 0);
    }

    private void applySetChange(EquipmentSet set, int oldPieces, int newPieces, boolean runTransitionActions) {
        int oldTier = set.effectiveTier(oldPieces);
        int newTier = set.effectiveTier(newPieces);
        if (oldTier == newTier) return;
        EquipmentSet.Entry oldEntry = set.entry(oldTier);
        if (oldEntry != null) {
            deactivateEntry(oldEntry, runTransitionActions);
        }
        EquipmentSet.Entry newEntry = set.entry(newTier);
        if (newEntry != null) {
            activateEntry(newEntry, runTransitionActions);
        }
    }

    private void activateEntry(EquipmentSet.Entry entry, boolean runTransitionActions) {
        this.holder.ifAttributesExist(attr -> {
            for (AttributeModifierConfig config : entry.modifiers()) {
                if (config.scope == AttributeModifierScope.WEAPON) continue;
                AttributeInstance instance = attr.getInstance(config.attribute);
                if (instance != null) {
                    instance.addOrUpdateModifier(config.build());
                }
            }
        });
        if (runTransitionActions) {
            entry.onActivate().run(this.holder.context);
        }
    }

    private void deactivateEntry(EquipmentSet.Entry entry, boolean runTransitionActions) {
        this.holder.ifAttributesExist(attr -> {
            for (AttributeModifierConfig config : entry.modifiers()) {
                if (config.scope == AttributeModifierScope.WEAPON) continue;
                AttributeInstance instance = attr.getInstance(config.attribute);
                if (instance != null) {
                    instance.removeModifier(config.id);
                }
            }
        });
        if (runTransitionActions) {
            entry.onDeactivate().run(this.holder.context);
        }
    }

    public void clearSetEffects() {
        this.activeSets = Map.of();
        this.dirty = false;
    }

    private Map<Key, Integer> computeRawActiveSets() {
        if (this.equipments.isEmpty()) return Map.of();
        Map<Key, Integer> setPartCount = new HashMap<>();
        for (Map.Entry<EquipmentSetSlot, EquipmentSlotItem> entry : this.equipments.entrySet()) {
            Item item = entry.getValue().item();
            Optional<ItemDefinition> definition = item.getDefinition();
            definition.ifPresent(def -> {
                EquipmentSetPart equipmentSetPart = def.settings().equipmentSetPart();
                if (equipmentSetPart != null) {
                    List<Key> matchingSets = equipmentSetPart.getMatchingSets(entry.getKey());
                    for (Key set : matchingSets) {
                        setPartCount.merge(set, 1, Integer::sum);
                    }
                }
            });
        }
        return setPartCount;
    }

    private void synchronizePotionEffects() {
        List<SetPotionEffect> effects = new ArrayList<>();
        for (Map.Entry<EquipmentSetSlot, EquipmentSlotItem> equipped : this.equipments.entrySet()) {
            effects.addAll(equipped.getValue().potionEffects());
        }
        for (Map.Entry<Key, Integer> activeSet : this.activeSets.entrySet()) {
            CraftEngine.instance().attributeManager().equipmentSet(activeSet.getKey())
                    .ifPresent(set -> {
                        int tier = set.effectiveTier(activeSet.getValue());
                        EquipmentSet.Entry entry = set.entry(tier);
                        if (entry == null) return;
                        effects.addAll(entry.potionEffects());
                    });
        }
        this.holder.potionEffects.update(effects);
    }
}
