package net.momirealms.craftengine.core.pack.model.legacy;

import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class LegacyItemModel {
    private final Key path;
    private final ModelGeneration generation;
    private final List<LegacyOverridesModel> overrides;

    public LegacyItemModel(Key path, List<LegacyOverridesModel> overrides, ModelGeneration generation) {
        this.generation = generation;
        this.path = path;
        this.overrides = overrides;
    }

    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        if (this.generation != null) {
            consumer.accept(new ModelGenerationHolder(this.path, this.generation));
        }
        for (LegacyOverridesModel override : this.overrides) {
            override.prepareModelGeneration(consumer);
        }
    }

    public List<LegacyOverridesModel> overrides() {
        return this.overrides;
    }

    public Key path() {
        return this.path;
    }

    private static final String[] PATH = new String[] {"path", "model"};

    public static LegacyItemModel fromConfig(ConfigSection section, int customModelData) {
        Key legacyModelPath = section.getNonNullAssetPath(PATH);
        ConfigSection generationSection = section.getSection("generation");
        ModelGeneration baseModelGeneration = null;
        if (generationSection != null) {
            baseModelGeneration = ModelGeneration.of(generationSection);
        }
        ConfigValue overridesValue = section.getValue("overrides");
        if (overridesValue != null) {
            List<LegacyOverridesModel> legacyOverridesModels = new ArrayList<>();
            legacyOverridesModels.add(new LegacyOverridesModel(null, legacyModelPath, customModelData, baseModelGeneration));
            overridesValue.forEach(v -> {
                ConfigSection overrideSection = v.getAsSection();
                Key overrideModelPath = overrideSection.getNonNullAssetPath(PATH);
                ConfigSection predicateSection = overrideSection.getNonNullSection("predicate");
                ConfigSection overrideGenerationSection = overrideSection.getSection("generation");
                ModelGeneration overrideModelGeneration = null;
                if (overrideGenerationSection != null) {
                    overrideModelGeneration = ModelGeneration.of(overrideGenerationSection);
                }
                legacyOverridesModels.add(new LegacyOverridesModel(predicateSection.values(), overrideModelPath, customModelData, overrideModelGeneration));
            });
            return new LegacyItemModel(
                    legacyModelPath,
                    legacyOverridesModels,
                    baseModelGeneration
            );
        } else {
            return new LegacyItemModel(
                    legacyModelPath,
                    List.of(new LegacyOverridesModel(null, legacyModelPath, customModelData, baseModelGeneration)),
                    baseModelGeneration
            );
        }
    }
}
