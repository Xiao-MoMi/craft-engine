package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.HappyGhastData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitCollider;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HappyGhastHitBox extends AbstractHitBox {
    public static final Factory FACTORY = new Factory();
    private final double scale;
    private final boolean hardCollision;
    private final List<Object> cachedValues = new ArrayList<>();

    public HappyGhastHitBox(Seat[] seats, Vector3f position, double scale, boolean canUseOn, boolean blocksBuilding, boolean canBeHitByProjectile, boolean hardCollision) {
        super(seats, position, canUseOn, blocksBuilding, canBeHitByProjectile);
        this.scale = scale;
        this.hardCollision = hardCollision;
        HappyGhastData.StaysStill.addEntityDataIfNotDefaultValue(hardCollision, this.cachedValues);
        HappyGhastData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, this.cachedValues); // NO AI
        HappyGhastData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedValues); // Invisible
    }

    @Override
    public Key type() {
        return HitBoxTypes.HAPPY_GHAST;
    }

    public double scale() {
        return scale;
    }

    public boolean hardCollision() {
        return hardCollision;
    }

    @Override
    public void initPacketsAndColliders(int[] entityIds, WorldPosition position, Quaternionf conjugated, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, BiConsumer<Integer, AABB> aabb) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        try {
            double x = position.x();
            double y = position.y();
            double z = position.z();
            float yaw = position.xRot();
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                    entityIds[0], UUID.randomUUID(), x + offset.x, y + offset.y, z - offset.z, 0, yaw,
                    MEntityTypes.HAPPY_GHAST, 0, CoreReflections.instance$Vec3$Zero, 0
            ), true);
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[0], List.copyOf(this.cachedValues)), true);
            if (VersionHelper.isOrAbove1_20_5() && this.scale != 1) {
                Object attributeInstance = CoreReflections.constructor$AttributeInstance.newInstance(MAttributeHolders.SCALE, (Consumer<?>) (o) -> {});
                CoreReflections.method$AttributeInstance$setBaseValue.invoke(attributeInstance, this.scale);
                packets.accept(NetworkReflections.constructor$ClientboundUpdateAttributesPacket0.newInstance(entityIds[0], Collections.singletonList(attributeInstance)), false);
            }
            if (this.hardCollision) {
                collider.accept(this.createCollider(position.world(), offset, x, y, z, entityIds[0], aabb));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to construct custom hitbox spawn packet", e);
        }
    }

    public Collider createCollider(World world, Vector3f offset, double x, double y, double z, int entityId, BiConsumer<Integer, AABB> aabb) {
        AABB ceAABB = createAABB(offset, x, y, z);
        Object level = world.serverWorld();
        Object nmsAABB = FastNMS.INSTANCE.constructor$AABB(ceAABB.minX, ceAABB.minY, ceAABB.minZ, ceAABB.maxX, ceAABB.maxY, ceAABB.maxZ);
        aabb.accept(entityId, ceAABB);
        return new BukkitCollider(level, nmsAABB, x, y, z, this.canBeHitByProjectile(), true, this.blocksBuilding());
    }

    public AABB createAABB(Vector3f offset, double x, double y, double z) {
        double baseSize = 4.0 * this.scale;
        double halfSize = baseSize * 0.5;
        double minX = x - halfSize + offset.x();
        double maxX = x + halfSize + offset.x();
        double minY = y + offset.y();
        double maxY = y + baseSize + offset.y();
        double minZ = z - halfSize - offset.z();
        double maxZ = z + halfSize - offset.z();
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void initShapeForPlacement(double x, double y, double z, float yaw, Quaternionf conjugated, Consumer<AABB> aabbs) {
        if (!this.hardCollision) return;
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        AABB aabb = createAABB(offset, x, y, z);
        aabbs.accept(aabb);
    }

    @Override
    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        return new int[] {entityIdSupplier.get()};
    }

    public static class Factory implements HitBoxFactory {

        @Override
        public HitBox create(Map<String, Object> arguments) {
            if (!VersionHelper.isOrAbove1_21_6()) {
                throw new UnsupportedOperationException("HappyGhastHitBox is only supported on 1.21.6+");
            }
            double scale = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("scale", 1), "scale");
            boolean hardCollision = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("hard-collision", true), "hard-collision");
            boolean canUseOn = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-use-item-on", true), "can-use-item-on");
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-hit-by-projectile", true), "can-be-hit-by-projectile");
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blocks-building", true), "blocks-building");
            return new HappyGhastHitBox(
                    HitBoxFactory.getSeats(arguments),
                    MiscUtils.getAsVector3f(arguments.getOrDefault("position", "0"), "position"),
                    scale, canUseOn, blocksBuilding, canBeHitByProjectile, hardCollision
            );
        }
    }
}
