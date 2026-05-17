package net.momirealms.craftengine.core.pack.model.generation;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public interface ModelGenerator {

    Map<Key, ModelGeneration> modelsToGenerate();

    void clearModelsToGenerate();
}
