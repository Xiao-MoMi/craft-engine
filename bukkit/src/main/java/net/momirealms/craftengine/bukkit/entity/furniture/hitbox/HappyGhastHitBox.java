package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HappyGhastHitBox extends AbstractHitBox {
    public static final Factory FACTORY = new Factory();
    private final double scale;

    public HappyGhastHitBox(Seat[] seats, Vector3f position, double scale, boolean canUseOn, boolean blocksBuilding, boolean canBeHitByProjectile) {
        super(seats, position, canUseOn, blocksBuilding, canBeHitByProjectile);
        this.scale = scale;
    }

    @Override
    public Key type() {
        return HitBoxTypes.HAPPY_GHAST;
    }

    public double scale() {
        return scale;
    }

    @Override
    public void initPacketsAndColliders(int[] entityId, WorldPosition position, Quaternionf conjugated, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, BiConsumer<Integer, AABB> aabb) {
    }

    @Override
    public void initShapeForPlacement(double x, double y, double z, float yaw, Quaternionf conjugated, Consumer<AABB> aabbs) {
    }

    @Override
    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        return new int[] {entityIdSupplier.get()};
    }

    public static class Factory implements HitBoxFactory {

        @Override
        public HitBox create(Map<String, Object> arguments) {
            double scale = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("scale", 1), "scale");
            boolean canUseOn = (boolean) arguments.getOrDefault("can-use-item-on", false);
            boolean canBeHitByProjectile = (boolean) arguments.getOrDefault("can-be-hit-by-projectile", false);
            boolean blocksBuilding = (boolean) arguments.getOrDefault("blocks-building", false);
            return new HappyGhastHitBox(
                    HitBoxFactory.getSeats(arguments),
                    MiscUtils.getAsVector3f(arguments.getOrDefault("position", "0"), "position"),
                    scale, canUseOn, blocksBuilding, canBeHitByProjectile
            );
        }
    }
}
