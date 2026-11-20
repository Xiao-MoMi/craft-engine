package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
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

public final class BetterModelProvider implements ModelProvider {

    @Override
    public List<NativeModelSource> bedrockModels() throws IOException {
        if (!Config.bedrockEntities().uses("bettermodel", BedrockModelIntegration.CUSTOM_ENTITY)) {
            return List.of();
        }
        return NativeModelSources.collect(
                "bettermodel",
                Bukkit.getPluginManager().getPlugin("BetterModel").getDataFolder().toPath().resolve("models"),
                BetterModel.models()
                        .stream()
                        .map(ModelRenderer::name)
                        .filter(name -> Config.bedrockEntities().mode(Key.of("bettermodel", name)) == BedrockModelIntegration.CUSTOM_ENTITY)
                        .toList()
        );
    }

    @Override
    public String plugin() {
        return "better_model";
    }

    @Override
    public ExternalModel createModel(String id) {
        if (!BetterModelUtils.hasModel(id)) return null;
        return new BetterModelModel(id);
    }

    @Override
    public int remapEntityId(int entityId) {
        return entityId;
    }
}
