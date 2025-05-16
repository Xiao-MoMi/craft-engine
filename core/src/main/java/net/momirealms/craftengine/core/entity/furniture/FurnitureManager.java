package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.incendo.cloud.suggestion.Suggestion;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

public interface FurnitureManager extends Manageable {
    String FURNITURE_ADMIN_NODE = "craftengine.furniture.admin";

    ConfigParser parser();

    void initSuggestions();

    Collection<Suggestion> cachedSuggestions();

    Furniture place(WorldPosition position, CustomFurniture furniture, FurnitureExtraData extraData, boolean playSound);

    Optional<CustomFurniture> furnitureById(Key id);

    boolean isFurnitureRealEntity(int entityId);

    @Nullable
    Furniture loadedFurnitureByRealEntityId(int entityId);

    @Nullable
    default Furniture loadedFurnitureByRealEntity(AbstractEntity entity) {
        return loadedFurnitureByRealEntityId(entity.entityID());
    }

    @Nullable
    Furniture loadedFurnitureByEntityId(int entityId);
}
