package net.momirealms.craftengine.core.entity.furniture.setting;

import net.momirealms.craftengine.core.util.Key;

public record FurnitureSettingsModifierType<M extends FurnitureSettingsModifier>(Key id, FurnitureSettingsModifierFactory<M> factory) {
}
