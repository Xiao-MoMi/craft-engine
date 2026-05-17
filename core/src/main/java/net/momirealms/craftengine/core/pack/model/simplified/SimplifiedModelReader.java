package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;

public interface SimplifiedModelReader {

    ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id);

    ItemModel read(ConfigValue modelValue);
}
