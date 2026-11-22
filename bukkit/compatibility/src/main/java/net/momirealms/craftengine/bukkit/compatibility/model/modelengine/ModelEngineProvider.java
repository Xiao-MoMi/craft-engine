package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelSource;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelSources;
import net.momirealms.craftengine.core.plugin.compatibility.ModelProvider;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.List;

public final class ModelEngineProvider implements ModelProvider {

    @Override
    public List<NativeModelSource> bedrockModels() throws IOException {
        if (!Config.bedrockEntities().uses("modelengine", BedrockModelIntegration.NATIVE_ENTITY)) {
            return List.of();
        }
        return NativeModelSources.collect(
                "modelengine",
                Bukkit.getPluginManager().getPlugin("ModelEngine").getDataFolder().toPath().resolve("blueprints"),
                ModelEngineAPI.getAPI().getModelRegistry().getValues()
                        .stream()
                        .map(ModelBlueprint::getName)
                        .filter(name -> Config.bedrockEntities().mode(Key.of("modelengine", name)) == BedrockModelIntegration.NATIVE_ENTITY)
                        .toList()
        );
    }

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
