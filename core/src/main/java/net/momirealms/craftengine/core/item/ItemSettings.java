package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemSettings {
    int fuelTime;
    Set<Key> tags = Set.of();

    private ItemSettings() {}

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

    public ItemSettings fuelTime(int fuelTime) {
        this.fuelTime = fuelTime;
        return this;
    }

    public ItemSettings tags(Set<Key> tags) {
        this.tags = tags;
        return this;
    }

    public interface Modifier {

        void apply(ItemSettings settings);

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
        }

        private static void registerFactory(String id, ItemSettings.Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}
