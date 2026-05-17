package net.momirealms.craftengine.core.entity.furniture.hitbox;

import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface FurnitureHitBox extends SeatOwner {

    Seat<FurnitureHitBox>[] seats();

    List<Collider> colliders();

    List<FurnitureHitboxPart> parts();

    void show(Player player);

    void hide(Player player);

    FurnitureHitBoxConfig<?> config();

    void collectInteractableEntityId(Consumer<Integer> collector);

    default Optional<EntityHitResult> clip(Vec3d min, Vec3d max) {
        for (FurnitureHitboxPart value : parts()) {
            Optional<EntityHitResult> clip = value.aabb().clip(min, max);
            if (clip.isPresent()) {
                return clip;
            }
        }
        return Optional.empty();
    }
}
