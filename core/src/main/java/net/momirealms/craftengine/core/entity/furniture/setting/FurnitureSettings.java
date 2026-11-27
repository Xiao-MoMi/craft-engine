package net.momirealms.craftengine.core.entity.furniture.setting;

import net.momirealms.craftengine.core.entity.furniture.FurnitureSounds;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.ExceptionCollector;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public final class FurnitureSettings {
    private final Object[] customData;
    FurnitureSounds sounds = FurnitureSounds.EMPTY;
    @Nullable
    Key itemId;
    int hitTimes;
    boolean allowBreakingInAdventureMode = false;
    LazyReference<Set<Key>> correctTools = LazyReference.untilNotNull(Set::of);

    private FurnitureSettings(Object[] customData) {
        this.customData = customData;
    }

    public static FurnitureSettings of() {
        return new FurnitureSettings(CustomFurnitureSettingType.newDataArray());
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
        FurnitureSettings newSettings = new FurnitureSettings(settings.customData.clone());
        newSettings.sounds = settings.sounds;
        newSettings.itemId = settings.itemId;
        newSettings.hitTimes = settings.hitTimes;
        return newSettings;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(CustomFurnitureSettingType<T> type) {
        return (T) this.customData[type.id()];
    }

    public void clearCustomData() {
        Arrays.fill(this.customData, null);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T removeCustomData(CustomFurnitureSettingType<?> type) {
        int id = type.id();
        Object value = this.customData[id];
        this.customData[id] = null;
        return (T) value;
    }

    public <T> void addCustomData(CustomFurnitureSettingType<T> key, T value) {
        this.customData[key.id()] = value;
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
