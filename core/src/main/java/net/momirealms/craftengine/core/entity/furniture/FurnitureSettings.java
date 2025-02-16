package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FurnitureSettings {
    FurnitureSounds sounds = FurnitureSounds.EMPTY;
    @Nullable
    Key itemId;

    private FurnitureSettings() {}

    public static FurnitureSettings of() {
        return new FurnitureSettings();
    }

    public static FurnitureSettings fromMap(Map<String, Object> map) {
        return applyModifiers(FurnitureSettings.of(), map);
    }

    public static FurnitureSettings ofFullCopy(FurnitureSettings settings) {
        FurnitureSettings newSettings = of();
        newSettings.sounds = settings.sounds;
        newSettings.itemId = settings.itemId;
        return newSettings;
    }

    public static FurnitureSettings applyModifiers(FurnitureSettings settings, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            FurnitureSettings.Modifier.Factory factory = FurnitureSettings.Modifiers.FACTORIES.get(entry.getKey());
            if (factory != null) {
                factory.createModifier(entry.getValue()).apply(settings);
            } else {
                throw new IllegalArgumentException("Unknown item settings key: " + entry.getKey());
            }
        }
        return settings;
    }

    public FurnitureSounds sounds() {
        return sounds;
    }

    @Nullable
    public Key itemId() {
        return itemId;
    }

    public FurnitureSettings sounds(FurnitureSounds sounds) {
        this.sounds = sounds;
        return this;
    }

    public FurnitureSettings itemId(Key itemId) {
        this.itemId = itemId;
        return this;
    }

    public interface Modifier {

        void apply(FurnitureSettings settings);

        interface Factory {

            FurnitureSettings.Modifier createModifier(Object value);
        }
    }

    public static class Modifiers {
        private static final Map<String, FurnitureSettings.Modifier.Factory> FACTORIES = new HashMap<>();

        static {
            registerFactory("sounds", (value -> {
                Map<String, Object> sounds = MiscUtils.castToMap(value, false);
                return settings -> settings.sounds(FurnitureSounds.fromMap(sounds));
            }));
            registerFactory("item", (value -> {
                String item = value.toString();
                return settings -> settings.itemId(Key.of(item));
            }));
        }

        private static void registerFactory(String id, FurnitureSettings.Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}
