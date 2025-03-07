package net.momirealms.craftengine.core.block;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generator.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface BlockManager extends Reloadable, ModelGenerator, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "blocks";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    Collection<ModelGeneration> modelsToGenerate();

    Map<Key, Map<String, JsonElement>> blockOverrides();

    Map<Key, CustomBlock> blocks();

    Optional<CustomBlock> getBlock(Key key);

    Collection<Suggestion> cachedSuggestions();

    Map<Key, Key> soundMapper();

    void initSuggestions();

    void delayedLoad();

    default int loadingSequence() {
        return LoadingSequence.BLOCK;
    }
}
