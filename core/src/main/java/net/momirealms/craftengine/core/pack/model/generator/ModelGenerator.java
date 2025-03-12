package net.momirealms.craftengine.core.pack.model.generator;

import java.util.Collection;

public interface ModelGenerator {
    Collection<ModelGeneration> modelsToGenerate();

    void clearModelsToGenerate();

    void prepareModelGeneration(ModelGeneration model);
}
