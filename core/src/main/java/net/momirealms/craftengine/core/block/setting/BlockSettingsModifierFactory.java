package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;

public interface BlockSettingsModifierFactory<M extends BlockSettingsModifier> {

    M create(ConfigValue value);
}
