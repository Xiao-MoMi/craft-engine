package net.momirealms.craftengine.core.plugin.compatibility;

import net.momirealms.craftengine.core.entity.furniture.ExternalModel;

public interface ModelProvider {

    String plugin();

    ExternalModel createModel(String id);

    int remapEntityId(int entityId);
}
