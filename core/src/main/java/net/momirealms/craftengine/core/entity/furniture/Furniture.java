package net.momirealms.craftengine.core.entity.furniture;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPosition;
import net.momirealms.craftengine.core.world.WorldPosition;

public interface Furniture {
    void initializeColliders();

    WorldPosition position();

    @NotNull float yaw();
    @NotNull float pitch();

    boolean isValid();

    void destroy();

    void destroySeats();

    Optional<Seat> findFirstAvailableSeat(int targetEntityId);

    boolean removeOccupiedSeat(Vector3f seat);

    default boolean removeOccupiedSeat(Seat seat) {
        return this.removeOccupiedSeat(seat.offset());
    }

    boolean tryOccupySeat(Seat seat);

    UUID uuid();

    int baseEntityId();

    @NotNull AnchorType anchorType();

    @NotNull Key id();

    @NotNull CustomFurniture config();

    boolean hasExternalModel();

    void spawnSeatEntityForPlayer(Player player, Seat seat);

    FurnitureExtraData extraData();

    void setExtraData(FurnitureExtraData extraData);

    void save();

    /**
     * Register a BlockStateHitBox position associated with this furniture
     * This information is persisted with the furniture entity
     * 
     * @param position The world position of the BlockStateHitBox
     */
    void addBlockStateHitBoxPosition(WorldPosition position);

    /**
     * Unregister a BlockStateHitBox position from this furniture
     * 
     * @param position The world position to remove
     * @return true if the position was removed, false if it wasn't registered
     */
    boolean removeBlockStateHitBoxPosition(WorldPosition position);

    /**
     * Get all BlockStateHitBox positions registered to this furniture
     * This method returns positions that are persisted with the furniture entity
     * 
     * @return Collection of block positions used by this furniture's BlockStateHitBoxes
     */
    Collection<BlockPosition> getBlockStateHitBoxPositions();

    /**
     * Check if this furniture has a BlockStateHitBox at the given position
     * 
     * @param position The world position to check
     * @return true if this furniture has a BlockStateHitBox at that position
     */
    boolean hasBlockStateHitBoxAt(WorldPosition position);

    /**
     * Clean up orphaned BlockStateHitBox positions that no longer correspond to actual blocks
     * This method can be called periodically or when issues are detected
     */
    default void cleanupOrphanedBlockStateHitBoxPositions() {
        // Default implementation does nothing, can be overridden by implementations
    }
}
