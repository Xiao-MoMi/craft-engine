package net.momirealms.craftengine.core.pack.model.generation;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractModelGenerator implements ModelGenerator {
    protected final CraftEngine plugin;
    protected final Map<Key, ModelGeneration> modelsToGenerate = new ConcurrentHashMap<>();

    public AbstractModelGenerator(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public Map<Key, ModelGeneration> modelsToGenerate() {
        return this.modelsToGenerate;
    }

    @Override
    public void clearModelsToGenerate() {
        this.modelsToGenerate.clear();
    }

    public void prepareModelGeneration(ModelGenerationHolder holder) {
        this.modelsToGenerate.compute(holder.path(), (k, conflict) -> {
            if (conflict != null && !conflict.equals(holder.model())) {
                throw new KnownResourceException("resource.model.generation_conflict", holder.path().asString());
            }
            return holder.model();
        });
    }
}
