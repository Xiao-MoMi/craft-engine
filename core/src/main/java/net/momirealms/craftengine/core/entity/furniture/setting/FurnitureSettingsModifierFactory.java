package net.momirealms.craftengine.core.entity.furniture.setting;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;

public interface FurnitureSettingsModifierFactory<M extends FurnitureSettingsModifier> {

    M create(ConfigValue value);
}
