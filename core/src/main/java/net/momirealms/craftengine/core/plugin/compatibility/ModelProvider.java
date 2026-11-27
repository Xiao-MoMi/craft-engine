package net.momirealms.craftengine.core.plugin.compatibility;

import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelSource;

import java.io.IOException;
import java.util.List;

public interface ModelProvider {

    String plugin();

    ExternalModel createModel(String id);

    int remapEntityId(int entityId);

    default List<NativeModelSource> bedrockModels() throws IOException {
        return List.of();
    }
}
