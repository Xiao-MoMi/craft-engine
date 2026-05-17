package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface FurnitureManager extends Manageable {
    Key FURNITURE_KEY = Key.ce("furniture_id");
    Key FURNITURE_EXTRA_DATA_KEY = Key.ce("furniture_extra_data");
    Key FURNITURE_COLLISION = Key.ce("collision");

    ConfigParser parser();

    void initSuggestions();

    Collection<Suggestion> cachedSuggestions();

    Furniture place(WorldPosition position, FurnitureDefinition furniture, FurniturePersistentData extraData, boolean playSound);

    Optional<FurnitureDefinition> furnitureById(Key id);

    Map<Key, FurnitureDefinition> loadedFurniture();

    boolean isFurnitureMetaEntity(int entityId);

    @Nullable
    Furniture loadedFurnitureByMetaEntityId(int entityId);

    @Nullable
    Furniture loadedFurnitureByInteractableEntityId(int entityId);

    @Nullable
    Furniture loadedFurnitureByColliderEntityId(int entityId);
}
