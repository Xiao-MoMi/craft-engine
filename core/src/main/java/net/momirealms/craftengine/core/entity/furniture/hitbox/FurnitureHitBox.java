package net.momirealms.craftengine.core.entity.furniture.hitbox;

import net.momirealms.craftengine.core.entity.furniture.ColliderConfig;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public interface FurnitureHitBox {
    @SuppressWarnings("unchecked")
    Seat<SeatOwner>[] EMPTY_SEATS = new Seat[0];

    default Seat<SeatOwner>[] seats() {
        return EMPTY_SEATS;
    }

    default int colliderConfigCount() {
        return 0;
    }

    default ColliderConfig colliderConfig(int index) {
        throw new IndexOutOfBoundsException(index);
    }

    default void collectCullingBounds(Consumer<AABB> consumer) {
        for (int i = 0, count = partCount(); i < count; i++) {
            consumer.accept(part(i).aabb());
        }
    }

    int partCount();

    FurnitureHitboxPart part(int index);

    default FurnitureHitboxPart findPart(int entityId) {
        for (int i = 0, count = partCount(); i < count; i++) {
            FurnitureHitboxPart part = part(i);
            if (part.entityId() == entityId) return part;
        }
        return null;
    }

    void show(Player player);

    void hide(Player player);

    default void showCulled(Player player) {}

    default void cull(Player player) {
        this.hide(player);
    }

    default void restore(Player player) {
        this.show(player);
    }

    void collectInteractableEntityId(IntConsumer collector);

    default boolean canUseItemOn() {
        return false;
    }

    default Optional<EntityHitResult> clip(Vec3d min, Vec3d max) {
        for (int i = 0, count = partCount(); i < count; i++) {
            FurnitureHitboxPart value = part(i);
            Optional<EntityHitResult> clip = value.aabb().clip(min, max);
            if (clip.isPresent()) {
                return clip;
            }
        }
        return Optional.empty();
    }
}
