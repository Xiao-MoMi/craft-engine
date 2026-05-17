package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.plugin.compatibility.ModelProvider;

public final class ModelEngineProvider implements ModelProvider {

    @Override
    public String plugin() {
        return "model_engine";
    }

    @Override
    public ExternalModel createModel(String id) {
        if (!ModelEngineUtils.hasModel(id)) return null;
        return new ModelEngineModel(id);
    }

    @Override
    public int remapEntityId(int entityId) {
        return ModelEngineUtils.interactionToBaseEntity(entityId);
    }
}
