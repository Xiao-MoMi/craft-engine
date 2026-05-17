package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;

public interface ItemSettingsModifierFactory<M extends ItemSettingsModifier> {

    M create(ConfigValue value);
}
