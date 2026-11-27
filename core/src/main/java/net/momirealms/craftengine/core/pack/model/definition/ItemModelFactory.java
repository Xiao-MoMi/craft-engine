package net.momirealms.craftengine.core.pack.model.definition;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.nio.file.Path;
import java.util.function.Consumer;

@FunctionalInterface
public interface ItemModelFactory<T extends ItemModel> {

    T create(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations);
}
