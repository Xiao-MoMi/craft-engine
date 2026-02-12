package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.CustomDataType;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class FurnitureSettings {
    boolean minimized;
    FurnitureSounds sounds = FurnitureSounds.EMPTY;
    @Nullable
    Key itemId;
    Map<CustomDataType<?>, Object> customData = new IdentityHashMap<>(4);
    int hitTimes;

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
        newSettings.minimized = settings.minimized;
        newSettings.hitTimes = settings.hitTimes;
        newSettings.customData = new IdentityHashMap<>(settings.customData);
        return newSettings;
    }

    public static FurnitureSettings applyModifiers(FurnitureSettings settings, Map<String, Object> map) {
        if (map == null) return settings;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            FurnitureSettings.Modifier.Factory factory = FurnitureSettings.Modifiers.FACTORIES.get(entry.getKey());
            if (factory != null) {
                factory.createModifier(entry.getValue()).apply(settings);
            } else {
                throw new LocalizedResourceConfigException("warning.config.furniture.settings.unknown", entry.getKey());
            }
        }
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(CustomDataType<T> type) {
        return (T) this.customData.get(type);
    }

    public void clearCustomData() {
        this.customData.clear();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T removeCustomData(CustomDataType<?> type) {
        return (T) this.customData.remove(type);
    }

    public <T> void addCustomData(CustomDataType<T> key, T value) {
        this.customData.put(key, value);
    }

    public FurnitureSounds sounds() {
        return this.sounds;
    }

    public boolean minimized() {
        return this.minimized;
    }

    @Nullable
    public Key itemId() {
        return this.itemId;
    }

    public int hitTimes() {
        return this.hitTimes;
    }

    public FurnitureSettings sounds(FurnitureSounds sounds) {
        this.sounds = sounds;
        return this;
    }

    public FurnitureSettings itemId(Key itemId) {
        this.itemId = itemId;
        return this;
    }

    public FurnitureSettings hitTimes(int hitTimes) {
        this.hitTimes = hitTimes;
        return this;
    }

    public FurnitureSettings minimized(boolean minimized) {
        this.minimized = minimized;
        return this;
    }

    @FunctionalInterface
    public interface Modifier {

        void apply(FurnitureSettings settings);

        @FunctionalInterface
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
            registerFactory("minimized", (value -> {
                boolean bool = (boolean) value;
                return settings -> settings.minimized(bool);
            }));
            registerFactory("hit-times", (value -> {
                int times = ResourceConfigUtils.getAsInt(value, "hit-times");
                return settings -> settings.hitTimes(times);
            }));
        }

        public static void registerFactory(String id, FurnitureSettings.Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}
