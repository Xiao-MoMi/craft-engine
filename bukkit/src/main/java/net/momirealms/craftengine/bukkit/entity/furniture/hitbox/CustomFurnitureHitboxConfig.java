package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.AbstractFurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigFactory;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CustomFurnitureHitboxConfig extends AbstractFurnitureHitBoxConfig<CustomFurnitureHitbox> {
    public static final Factory FACTORY = new Factory();
    private final float scale;
    private final Object entityType;
    private final List<Object> cachedValues = new ArrayList<>();
    private final float width;
    private final float height;

    private CustomFurnitureHitboxConfig(SeatConfig[] seats,
                                       Vector3f position,
                                       boolean canUseItemOn,
                                       boolean blocksBuilding,
                                       boolean canBeHitByProjectile,
                                       float width,
                                       float height,
                                       boolean fixed,
                                       float scale,
                                       Object type) {
        super(seats, position, canUseItemOn, blocksBuilding, canBeHitByProjectile);
        this.scale = scale;
        this.entityType = type;
        this.width = fixed ? width : width * scale;
        this.height = fixed ? height : height * scale;
        BaseEntityData.NoGravity.addEntityDataIfNotDefaultValue(true, this.cachedValues);
        BaseEntityData.Silent.addEntityDataIfNotDefaultValue(true, this.cachedValues);
        BaseEntityData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedValues);
    }

    public float scale() {
        return this.scale;
    }

    public Object entityType() {
        return this.entityType;
    }

    public List<Object> cachedValues() {
        return this.cachedValues;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    @Override
    public void prepareBoundingBox(WorldPosition targetPos, Consumer<AABB> aabbConsumer, boolean ignoreBlocksBuilding) {
        if (this.blocksBuilding || ignoreBlocksBuilding) {
            Vec3d relativePosition = Furniture.getRelativePosition(targetPos, this.position);
            aabbConsumer.accept(AABB.makeBoundingBox(relativePosition, this.width, this.height));
        }
    }

    @Override
    public CustomFurnitureHitbox create(Furniture furniture) {
        return new CustomFurnitureHitbox(furniture, this);
    }

    public static class Factory implements FurnitureHitBoxConfigFactory<CustomFurnitureHitbox> {

        @Override
        public CustomFurnitureHitboxConfig create(Map<String, Object> arguments) {
            Vector3f position = ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", "0"), "position");
            float scale = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("scale", 1), "scale");
            String type = (String) arguments.getOrDefault("entity-type", "slime");
            Object nmsEntityType = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ENTITY_TYPE, KeyUtils.toIdentifier(Key.of(type)));
            if (nmsEntityType == null) {
                throw new LocalizedResourceConfigException("warning.config.furniture.hitbox.custom.invalid_entity", new IllegalArgumentException("EntityType not found: " + type), type);
            }
            float width;
            float height;
            boolean fixed;
            try {
                Object dimensions = CoreReflections.field$EntityType$dimensions.get(nmsEntityType);
                width = CoreReflections.field$EntityDimensions$width.getFloat(dimensions);
                height = CoreReflections.field$EntityDimensions$height.getFloat(dimensions);
                fixed = CoreReflections.field$EntityDimensions$fixed.getBoolean(dimensions);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to get dimensions for " + nmsEntityType, e);
            }
            boolean canUseItemOn = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-use-item-on", false), "can-use-item-on");
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-hit-by-projectile", false), "can-be-hit-by-projectile");
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blocks-building", true), "blocks-building");
            return new CustomFurnitureHitboxConfig(SeatConfig.fromObj(arguments.get("seats")), position, canUseItemOn, blocksBuilding, canBeHitByProjectile, width, height, fixed, scale, nmsEntityType);
        }
    }
}
