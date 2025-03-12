package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.item.modifier.EquippableModifier;
import net.momirealms.craftengine.core.item.modifier.ItemModifier;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSettings {
    int fuelTime;
    Set<Key> tags = Set.of();
    @Nullable
    EquipmentData equipmentData;

    private ItemSettings() {}

    public <I> List<ItemModifier<I>> modifiers() {
        if (this.equipmentData == null) return Collections.emptyList();
        return List.of(new EquippableModifier<>(this.equipmentData));
    }

    public static ItemSettings of() {
        return new ItemSettings();
    }

    public static ItemSettings fromMap(Map<String, Object> map) {
        return applyModifiers(ItemSettings.of(), map);
    }

    public static ItemSettings ofFullCopy(ItemSettings settings) {
        ItemSettings newSettings = of();
        newSettings.fuelTime = settings.fuelTime;
        newSettings.tags = settings.tags;
        newSettings.equipmentData = settings.equipmentData;
        return newSettings;
    }

    public static ItemSettings applyModifiers(ItemSettings settings, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            ItemSettings.Modifier.Factory factory = ItemSettings.Modifiers.FACTORIES.get(entry.getKey());
            if (factory != null) {
                factory.createModifier(entry.getValue()).apply(settings);
            } else {
                throw new IllegalArgumentException("Unknown item settings key: " + entry.getKey());
            }
        }
        return settings;
    }

    public int fuelTime() {
        return fuelTime;
    }

    public Set<Key> tags() {
        return tags;
    }

    @Nullable
    public EquipmentData equipmentData() {
        return equipmentData;
    }

    public ItemSettings fuelTime(int fuelTime) {
        this.fuelTime = fuelTime;
        return this;
    }

    public ItemSettings tags(Set<Key> tags) {
        this.tags = tags;
        return this;
    }

    public ItemSettings equipmentData(EquipmentData equipmentData) {
        this.equipmentData = equipmentData;
        return this;
    }

    @FunctionalInterface
    public interface Modifier {

        void apply(ItemSettings settings);

        @FunctionalInterface
        interface Factory {

            ItemSettings.Modifier createModifier(Object value);
        }
    }

    public static class Modifiers {
        private static final Map<String, ItemSettings.Modifier.Factory> FACTORIES = new HashMap<>();

        static {
            registerFactory("fuel-time", (value -> {
                int intValue = MiscUtils.getAsInt(value);
                return settings -> settings.fuelTime(intValue);
            }));
            registerFactory("tags", (value -> {
                List<String> tags = MiscUtils.getAsStringList(value);
                return settings -> settings.tags(tags.stream().map(Key::of).collect(Collectors.toSet()));
            }));
            registerFactory("equippable", (value -> {
                Map<String, Object> data = MiscUtils.castToMap(value, false);
                String slot = (String) data.get("slot");
                if (slot == null) {
                    throw new IllegalArgumentException("No slot specified");
                }
                EquipmentSlot slotEnum = EquipmentSlot.valueOf(slot.toUpperCase(Locale.ENGLISH));
                EquipmentData.Builder builder = EquipmentData.builder().slot(slotEnum);
                if (data.containsKey("asset-id")) {
                    builder.assetId(Key.of(data.get("asset-id").toString()));
                }
                if (data.containsKey("camera-overlay")) {
                    builder.cameraOverlay(Key.of(data.get("camera-overlay").toString()));
                }
                return settings -> settings.equipmentData(builder.build());
            }));
        }

        private static void registerFactory(String id, ItemSettings.Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}
