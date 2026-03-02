package net.momirealms.craftengine.core.pack.model.legacy;

import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class LegacyItemModel {
    private final List<ModelGeneration> modelsToGenerate;
    private final Key path;
    private final List<LegacyOverridesModel> overrides;

    public LegacyItemModel(Key path, List<LegacyOverridesModel> overrides, List<ModelGeneration> modelsToGenerate) {
        this.modelsToGenerate = modelsToGenerate;
        this.path = path;
        this.overrides = overrides;
    }

    public List<ModelGeneration> modelsToGenerate() {
        return modelsToGenerate;
    }

    public List<LegacyOverridesModel> overrides() {
        return overrides;
    }

    public Key path() {
        return path;
    }

    public static LegacyItemModel fromConfig(ConfigSection legacyModel, int customModelData) {
        Key legacyModelPath = legacyModel.getNonNullIdentifier("path");
        ConfigSection generation = legacyModel.getSection("generation");
        ModelGeneration baseModelGeneration = null;
        if (generation != null) {
            baseModelGeneration = ModelGeneration.of(legacyModelPath, generation);
        }
        List<ConfigSection> overrides = legacyModel.parseSectionList(it -> it, "overrides");
        if (!overrides.isEmpty()) {
            List<ModelGeneration> modelGenerations = new ArrayList<>();
            List<LegacyOverridesModel> legacyOverridesModels = new ArrayList<>();
            if (baseModelGeneration != null) modelGenerations.add(baseModelGeneration);
            legacyOverridesModels.add(new LegacyOverridesModel(new HashMap<>(), legacyModelPath, customModelData));
            for (ConfigSection override : overrides) {
                Key overrideModelPath = override.getNonNullIdentifier("path");
                ConfigSection predicate = override.getNonNullSection("predicate");
                if (predicate.values().isEmpty()) {
                    throw new LocalizedResourceConfigException("warning.config.item.legacy_model.overrides.missing_predicate");
                }
                ConfigSection overrideGeneration = override.getSection("generation");
                if (overrideGeneration != null) {
                    modelGenerations.add(ModelGeneration.of(overrideModelPath, overrideGeneration));
                }
                legacyOverridesModels.add(new LegacyOverridesModel(predicate.values(), overrideModelPath, customModelData));
            }
            return new LegacyItemModel(legacyModelPath, legacyOverridesModels, modelGenerations);
        } else {
            return new LegacyItemModel(legacyModelPath,
                    List.of(new LegacyOverridesModel(new HashMap<>(), legacyModelPath, customModelData)),
                    baseModelGeneration == null ? List.of() : List.of(baseModelGeneration)
            );
        }
    }
}
