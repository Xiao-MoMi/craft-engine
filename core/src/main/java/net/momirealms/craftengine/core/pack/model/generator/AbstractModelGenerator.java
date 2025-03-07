package net.momirealms.craftengine.core.pack.model.generator;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractModelGenerator implements ModelGenerator {
    protected final CraftEngine plugin;
    protected final Map<Key, ModelGeneration> modelsToGenerate = new HashMap<>();

    public AbstractModelGenerator(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<ModelGeneration> modelsToGenerate() {
        return this.modelsToGenerate.values();
    }

    @Override
    public void clearModelsToGenerate() {
        this.modelsToGenerate.clear();
    }

    @Override
    public void prepareModelGeneration(ModelGeneration model) {
        ModelGeneration generation = this.modelsToGenerate.get(model.path());
        if (generation != null) {
            if (generation.equals(model)) {
                return;
            }
            this.plugin.logger().severe("Two or more configurations attempt to generate different json models with the same path: " + model.path());
            return;
        }
        this.modelsToGenerate.put(model.path(), model);
    }
}
