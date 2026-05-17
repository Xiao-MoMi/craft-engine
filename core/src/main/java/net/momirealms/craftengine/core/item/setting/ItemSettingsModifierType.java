package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.util.Key;

public record ItemSettingsModifierType<M extends ItemSettingsModifier>(Key id, ItemSettingsModifierFactory<M> factory) {
}
