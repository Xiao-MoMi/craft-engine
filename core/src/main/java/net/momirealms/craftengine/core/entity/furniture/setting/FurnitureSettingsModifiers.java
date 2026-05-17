package net.momirealms.craftengine.core.entity.furniture.setting;

import net.momirealms.craftengine.core.entity.furniture.FurnitureSounds;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FurnitureSettingsModifiers {
    public static final FurnitureSettingsModifierType<FurnitureSettingsModifier> ITEM = register(Key.ce("item"), value -> {
        Key itemId = value.getAsIdentifier();
        return settings -> settings.itemId = itemId;
    });
    public static final FurnitureSettingsModifierType<FurnitureSettingsModifier> SOUNDS = register(Key.ce("sounds"), value -> {
        FurnitureSounds sounds = FurnitureSounds.fromConfig(value.getAsSection());
        return settings -> settings.sounds = sounds;
    });
    public static final FurnitureSettingsModifierType<FurnitureSettingsModifier> HIT_TIMES = register(Key.ce("hit_times"), value -> {
        return settings -> settings.hitTimes = value.getAsInt();
    });
    public static final FurnitureSettingsModifierType<FurnitureSettingsModifier> ADVENTURE_MODE_BREAKING = register(Key.ce("adventure_mode_breaking"), value -> {
        return settings -> settings.allowBreakingInAdventureMode = value.getAsBoolean();
    });
    public static final FurnitureSettingsModifierType<FurnitureSettingsModifier> CORRECT_TOOLS = register(Key.ce("correct_tools"), value -> {
        List<String> tools = value.getAsStringList();
        LazyReference<Set<Key>> correctTools = LazyReference.lazyReference(() -> {
            Set<Key> ids = new HashSet<>();
            for (String tool : tools) {
                if (tool.charAt(0) == '#') ids.addAll(CraftEngine.instance().itemManager().itemIdsByTag(Key.of(tool.substring(1))).stream().map(UniqueKey::key).toList());
                else ids.add(Key.of(tool));
            }
            return ids;
        });
        return settings -> settings.correctTools(correctTools);
    });

    private FurnitureSettingsModifiers() {}

    public static void init() {}

    public static <M extends FurnitureSettingsModifier> FurnitureSettingsModifierType<M> register(Key id, FurnitureSettingsModifierFactory<M> factory) {
        FurnitureSettingsModifierType<M> type = new FurnitureSettingsModifierType<>(id, factory);
        ((WritableRegistry<FurnitureSettingsModifierType<? extends FurnitureSettingsModifier>>) BuiltInRegistries.FURNITURE_SETTINGS_TYPE)
                .register(ResourceKey.create(Registries.FURNITURE_SETTINGS_TYPE.location(), id), type);
        return type;
    }
}
