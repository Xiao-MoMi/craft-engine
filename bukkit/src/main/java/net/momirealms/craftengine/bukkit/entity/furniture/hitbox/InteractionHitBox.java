package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.InteractionEntityData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitCollider;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class InteractionHitBox extends AbstractHitBox {
    public static final Factory FACTORY = new Factory();
    public static final InteractionHitBox DEFAULT = new InteractionHitBox(new Seat[0], new Vector3f(), new Vector3f(1,1,1), true, false, false, false);

    private final Vector3f size;
    private final boolean responsive;
    private final List<Object> cachedValues = new ArrayList<>();

    public InteractionHitBox(Seat[] seats, Vector3f position, Vector3f size, boolean responsive, boolean canUseOn, boolean blocksBuilding, boolean canBeHitByProjectile) {
        super(seats, position, canUseOn, blocksBuilding, canBeHitByProjectile);
        this.size = size;
        this.responsive = responsive;
        InteractionEntityData.Height.addEntityDataIfNotDefaultValue(size.y, cachedValues);
        InteractionEntityData.Width.addEntityDataIfNotDefaultValue(size.x, cachedValues);
        InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(responsive, cachedValues);
    }

    public boolean responsive() {
        return responsive;
    }

    public Vector3f size() {
        return size;
    }

    @Override
    public Key type() {
        return HitBoxTypes.INTERACTION;
    }

    @Override
    public void initPacketsAndColliders(int[] entityId, WorldPosition position, Quaternionf conjugated, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, BiConsumer<Integer, AABB> aabb) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        double x = position.x();
        double y = position.y();
        double z = position.z();
        float yaw = position.xRot();
        packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId[0], UUID.randomUUID(), x + offset.x, y + offset.y, z - offset.z, 0, yaw,
                MEntityTypes.INTERACTION, 0, CoreReflections.instance$Vec3$Zero, 0
        ), true);
        packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId[0], List.copyOf(this.cachedValues)), true);
        if (canUseItemOn()) {
            aabb.accept(entityId[0], AABB.fromInteraction(new Vec3d(x + offset.x, y + offset.y, z - offset.z), this.size.x, this.size.y));
        }
        if (blocksBuilding() || this.canBeHitByProjectile()) {
            AABB ceAABB = AABB.fromInteraction(new Vec3d(x + offset.x, y + offset.y, z - offset.z), this.size.x, this.size.y);
            Object nmsAABB = FastNMS.INSTANCE.constructor$AABB(ceAABB.minX, ceAABB.minY, ceAABB.minZ, ceAABB.maxX, ceAABB.maxY, ceAABB.maxZ);
            collider.accept(new BukkitCollider(position.world().serverWorld(), nmsAABB, x, y, z, this.canBeHitByProjectile(), false, this.blocksBuilding()));
        }
    }

    @Override
    public void initShapeForPlacement(double x, double y, double z, float yaw, Quaternionf conjugated, Consumer<AABB> aabbs) {
        if (blocksBuilding()) {
            Vector3f offset = conjugated.transform(new Vector3f(position()));
            AABB ceAABB = AABB.fromInteraction(new Vec3d(x + offset.x, y + offset.y, z - offset.z), this.size.x, this.size.y);
            aabbs.accept(ceAABB);
        }
    }

    @Override
    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        return new int[] {entityIdSupplier.get()};
    }

    public static class Factory implements HitBoxFactory {

        @Override
        public HitBox create(Map<String, Object> arguments) {
            Vector3f position = MiscUtils.getAsVector3f(arguments.getOrDefault("position", "0"), "position");
            float width;
            float height;
            if (arguments.containsKey("scale")) {
                String[] split = arguments.get("scale").toString().split(",");
                width = Float.parseFloat(split[0]);
                height = Float.parseFloat(split[1]);
            } else {
                width = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("width", "1"), "width");
                height = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("height", "1"), "height");
            }
            boolean canUseOn = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-use-item-on", false), "can-use-item-on");
            boolean interactive = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("interactive", true), "interactive");
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-hit-by-projectile", false), "can-be-hit-by-projectile");
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blocks-building", true), "blocks-building");
            return new InteractionHitBox(
                    HitBoxFactory.getSeats(arguments),
                    position,
                    new Vector3f(width, height, width),
                    interactive, canUseOn, blocksBuilding, canBeHitByProjectile
            );
        }
    }
}
