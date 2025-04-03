package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.Collection;
import java.util.Optional;

public interface FurnitureManager extends Reloadable, ConfigSectionParser {
    String[] CONFIG_SECTION_NAME = new String[] { "furniture" };
    String FURNITURE_ADMIN_NODE = "craftengine.furniture.admin";

    void delayedLoad();

    void initSuggestions();

    Collection<Suggestion> cachedSuggestions();

    void delayedInit();

    @Override
    default String[] sectionId() {
        return CONFIG_SECTION_NAME;
    }

    @Override
    default int loadingSequence() {
        return LoadingSequence.FURNITURE;
    }

    Optional<CustomFurniture> getFurniture(Key id);

    boolean isFurnitureRealEntity(int entityId);
}
