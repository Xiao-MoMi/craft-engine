package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomHitBox extends AbstractHitBox {
    public static final Factory FACTORY = new Factory();
    private final float scale;
    private final EntityType entityType;
    private final List<Object> cachedValues = new ArrayList<>();

    public CustomHitBox(Seat[] seats, Vector3f position, EntityType type, float scale, boolean blocksBuilding, boolean canBeHitByProjectile) {
        super(seats, position, false, blocksBuilding, canBeHitByProjectile);
        this.scale = scale;
        this.entityType = type;
        BaseEntityData.NoGravity.addEntityDataIfNotDefaultValue(true, this.cachedValues);
        BaseEntityData.Silent.addEntityDataIfNotDefaultValue(true, this.cachedValues);
        BaseEntityData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedValues);
    }

    public EntityType entityType() {
        return entityType;
    }

    public float scale() {
        return scale;
    }

    @Override
    public Key type() {
        return HitBoxTypes.CUSTOM;
    }

    @Override
    public void initPacketsAndColliders(int[] entityId, WorldPosition position, Quaternionf conjugated, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, BiConsumer<Integer, AABB> aabb) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        try {
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                    entityId[0], UUID.randomUUID(), position.x() + offset.x, position.y() + offset.y, position.z() - offset.z, 0, position.xRot(),
                    FastNMS.INSTANCE.method$CraftEntityType$toNMSEntityType(this.entityType), 0, CoreReflections.instance$Vec3$Zero, 0
            ), true);
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId[0], List.copyOf(this.cachedValues)), true);
            if (VersionHelper.isOrAbove1_20_5() && this.scale != 1) {
                Object attributeInstance = CoreReflections.constructor$AttributeInstance.newInstance(MAttributeHolders.SCALE, (Consumer<?>) (o) -> {});
                CoreReflections.method$AttributeInstance$setBaseValue.invoke(attributeInstance, this.scale);
                packets.accept(NetworkReflections.constructor$ClientboundUpdateAttributesPacket0.newInstance(entityId[0], Collections.singletonList(attributeInstance)), false);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to construct custom hitbox spawn packet", e);
        }
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
            Vector3f position = MiscUtils.getAsVector3f(arguments.getOrDefault("position", "0"), "position");
            float scale = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("scale", 1), "scale");
            String type = (String) arguments.getOrDefault("entity-type", "slime");
            EntityType entityType = Registry.ENTITY_TYPE.get(new NamespacedKey("minecraft", type));
            if (entityType == null) {
                throw new LocalizedResourceConfigException("warning.config.furniture.hitbox.custom.invalid_entity", new IllegalArgumentException("EntityType not found: " + type), type);
            }
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-hit-by-projectile", false), "can-be-hit-by-projectile");
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blocks-building", true), "blocks-building");
            return new CustomHitBox(HitBoxFactory.getSeats(arguments), position, entityType, scale, blocksBuilding, canBeHitByProjectile);
        }
    }
}
