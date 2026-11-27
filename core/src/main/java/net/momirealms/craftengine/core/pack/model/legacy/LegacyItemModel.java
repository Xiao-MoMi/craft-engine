package net.momirealms.craftengine.core.pack.model.legacy;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class LegacyItemModel {
    private final Key path;
    private final List<LegacyOverridesModel> overrides;

    public LegacyItemModel(Key path, List<LegacyOverridesModel> overrides) {
        this.path = path;
        this.overrides = overrides;
    }

    public List<LegacyOverridesModel> overrides() {
        return this.overrides;
    }

    public Key path() {
        return this.path;
    }

    private static final String[] PATH = ConfigKeys.of("path|model");

    public static LegacyItemModel fromConfig(Pack pack, Path path, ConfigSection section, int customModelData, Consumer<ModelGenerationHolder> generations) {
        Key legacyModelPath = parseModelPath(pack, path, section, generations);
        List<LegacyOverridesModel> overrides = new ArrayList<>();
        overrides.add(new LegacyOverridesModel(null, legacyModelPath, customModelData));
        ConfigValue overridesValue = section.getValue("overrides");
        if (overridesValue != null) {
            overridesValue.forEach(value -> {
                ConfigSection overrideSection = value.getAsSection();
                Key overridePath = parseModelPath(pack, path, overrideSection, generations);
                ConfigSection predicateSection = overrideSection.getNonNullSection("predicate");
                overrides.add(new LegacyOverridesModel(predicateSection.values(), overridePath, customModelData));
            });
        }
        return new LegacyItemModel(legacyModelPath, overrides);
    }

    private static Key parseModelPath(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
        ConfigValue blueprintValue = section.getValue("blueprint");
        if (blueprintValue != null) {
            BBModelConverter.Converted converted = BBModelConverter.convert(pack, path, "item", section.getValue(PATH), blueprintValue);
            generations.accept(new ModelGenerationHolder(converted.model(), ModelGeneration.raw(converted.json(), converted.textures(), converted.textureMetadata())));
            return converted.model();
        }
        Key modelPath = section.getNonNullValue(PATH, ConfigConstants.ARGUMENT_IDENTIFIER).getAsAssetPath();
        ConfigSection generationSection = section.getSection("generation");
        if (generationSection != null) {
            generations.accept(new ModelGenerationHolder(modelPath, ModelGeneration.of(generationSection)));
        }
        return modelPath;
    }
}
