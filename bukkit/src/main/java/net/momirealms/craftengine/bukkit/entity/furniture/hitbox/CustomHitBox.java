package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
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

    public CustomHitBox(Seat[] seats, Vector3f position, EntityType type, float scale) {
        super(seats, position);
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
    public void initPacketsAndColliders(int[] entityId, double x, double y, double z, float yaw, Quaternionf conjugated, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        try {
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                    entityId[0], UUID.randomUUID(), x + offset.x, y + offset.y, z - offset.z, 0, yaw,
                    FastNMS.INSTANCE.toNMSEntityType(this.entityType), 0, Reflections.instance$Vec3$Zero, 0
            ), true);
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId[0], List.copyOf(this.cachedValues)), true);
            if (VersionHelper.isVersionNewerThan1_20_5() && this.scale != 1) {
                Object attributeInstance = Reflections.constructor$AttributeInstance.newInstance(Reflections.instance$Holder$Attribute$scale, (Consumer<?>) (o) -> {});
                Reflections.method$AttributeInstance$setBaseValue.invoke(attributeInstance, this.scale);
                packets.accept(Reflections.constructor$ClientboundUpdateAttributesPacket0.newInstance(entityId[0], Collections.singletonList(attributeInstance)), false);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to construct custom hitbox spawn packet", e);
        }
    }

    @Override
    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        return new int[] {entityIdSupplier.get()};
    }

    public static class Factory implements HitBoxFactory {

        @Override
        public HitBox create(Map<String, Object> arguments) {
            Vector3f position = MiscUtils.getVector3f(arguments.getOrDefault("position", "0"));
            float scale = MiscUtils.getAsFloat(arguments.getOrDefault("scale", "1"));
            EntityType entityType = Registry.ENTITY_TYPE.get(new NamespacedKey("minecraft", (String) arguments.getOrDefault("entity-type", "slime")));
            if (entityType == null) {
                throw new IllegalArgumentException("EntityType not found: " + arguments.get("entity-type"));
            }
            return new CustomHitBox(HitBoxFactory.getSeats(arguments), position, entityType, scale);
        }
    }
}
