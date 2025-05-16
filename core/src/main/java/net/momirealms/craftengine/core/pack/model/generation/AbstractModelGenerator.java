package net.momirealms.craftengine.core.pack.model.generation;

import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
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

    public void prepareModelGeneration(ModelGeneration model) {
        ModelGeneration conflict = this.modelsToGenerate.get(model.path());
        if (conflict != null) {
            if (conflict.equals(model)) {
                return;
            }
            throw new LocalizedResourceConfigException("warning.config.model.generation.conflict", model.path().toString());
        }
        if (!ResourceLocation.isValid(model.parentModelPath())) {
            throw new LocalizedResourceConfigException("warning.config.model.generation.parent.invalid", model.parentModelPath());
        }
        Map<String, String> textures = model.texturesOverride();
        if (textures != null) {
            for (Map.Entry<String, String> texture : textures.entrySet()) {
                if (texture.getValue().charAt(0) != '#') {
                    if (!ResourceLocation.isValid(texture.getValue())) {
                        throw new LocalizedResourceConfigException("warning.config.model.generation.texture.invalid", texture.getKey(), texture.getValue());
                    }
                }
            }
        }
        this.modelsToGenerate.put(model.path(), model);
    }
}
