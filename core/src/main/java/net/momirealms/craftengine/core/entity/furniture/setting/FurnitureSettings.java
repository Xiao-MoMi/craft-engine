package net.momirealms.craftengine.core.entity.furniture.setting;

import net.momirealms.craftengine.core.entity.furniture.FurnitureSounds;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FurnitureSettings {
    FurnitureSounds sounds = FurnitureSounds.EMPTY;
    @Nullable
    Key itemId;
    Map<CustomDataType<?>, Object> customData = new IdentityHashMap<>(4);
    int hitTimes;
    boolean allowBreakingInAdventureMode = false;
    LazyReference<Set<Key>> correctTools = LazyReference.lazyReference(Set::of);

    private FurnitureSettings() {}

    public static FurnitureSettings of() {
        return new FurnitureSettings();
    }

    public static FurnitureSettings fromConfig(ConfigSection section) {
        FurnitureSettings furnitureSettings = FurnitureSettings.of();
        if (section == null) return furnitureSettings;
        applyModifiers(furnitureSettings, section);
        return furnitureSettings;
    }

    public static void applyModifiers(FurnitureSettings settings, ConfigSection section) {
        ExceptionCollector<KnownResourceException> collector = new ExceptionCollector<>(KnownResourceException.class);
        if (section != null) {
            for (String type : section.keySet()) {
                ConfigValue value = section.getValue(type);
                if (value == null) continue;
                String key = StringUtils.normalizeSettingsType(type);
                collector.runCatching(() -> {
                    Optional.ofNullable(BuiltInRegistries.FURNITURE_SETTINGS_TYPE.getValue(Key.ce(key)))
                            .ifPresent(modifierType ->
                                    modifierType.factory().create(value).apply(settings));
                });
            }
        }
        collector.throwIfPresent();
    }

    public static FurnitureSettings ofFullCopy(FurnitureSettings settings) {
        FurnitureSettings newSettings = of();
        newSettings.sounds = settings.sounds;
        newSettings.itemId = settings.itemId;
        newSettings.hitTimes = settings.hitTimes;
        newSettings.customData = new IdentityHashMap<>(settings.customData);
        return newSettings;
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

    @Nullable
    public Key itemId() {
        return this.itemId;
    }

    public int hitTimes() {
        return this.hitTimes;
    }

    public boolean allowBreakingInAdventureMode() {
        return this.allowBreakingInAdventureMode;
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

    public FurnitureSettings allowBreakingInAdventureMode(boolean allowBreaking) {
        this.allowBreakingInAdventureMode = allowBreaking;
        return this;
    }

    public FurnitureSettings correctTools(LazyReference<Set<Key>> correctTools) {
        this.correctTools = correctTools;
        return this;
    }
}
