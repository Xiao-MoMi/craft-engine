package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;

import javax.annotation.Nullable;
import java.util.Optional;

public interface FurnitureManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "furniture";

    void delayedInit();

    @Override
    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    @Override
    default int loadingSequence() {
        return LoadingSequence.FURNITURE;
    }

    Optional<CustomFurniture> getFurniture(Key id);

    @Nullable
    int[] getSubEntityIdsByBaseEntityId(int entityId);

    boolean isFurnitureBaseEntity(int entityId);
}
