package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.util.Key;

public record BlockSettingsModifierType<M extends BlockSettingsModifier>(Key id, BlockSettingsModifierFactory<M> factory) {
}
