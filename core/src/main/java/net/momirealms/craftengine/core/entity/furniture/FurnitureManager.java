package net.momirealms.craftengine.core.entity.furniture;

import java.util.Collection;
import java.util.Optional;

import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.Nullable;

import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPosition;
import net.momirealms.craftengine.core.world.WorldPosition;

public interface FurnitureManager extends Manageable {
    Key FURNITURE_KEY = Key.of("craftengine:furniture_id");
    Key FURNITURE_EXTRA_DATA_KEY = Key.of("craftengine:furniture_extra_data");
    Key FURNITURE_SEAT_BASE_ENTITY_KEY = Key.of("craftengine:seat_to_base_entity");
    Key FURNITURE_SEAT_VECTOR_3F_KEY = Key.of("craftengine:seat_vector");
    Key FURNITURE_COLLISION = Key.of("craftengine:collision");

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

    // Methods for BlockStateHitBox tracking
    void registerBlockStateHitBox(WorldPosition position, Furniture furniture);
    
    void unregisterBlockStateHitBox(WorldPosition position);
    
    @Nullable
    Furniture getFurnitureByBlockPosition(WorldPosition position);
    
    Collection<BlockPosition> getBlockStateHitBoxPositions(Furniture furniture);
}
