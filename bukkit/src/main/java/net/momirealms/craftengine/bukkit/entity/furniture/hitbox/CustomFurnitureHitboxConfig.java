package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.AbstractFurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigFactory;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityDimensionsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CustomFurnitureHitboxConfig extends AbstractFurnitureHitBoxConfig<CustomFurnitureHitbox> {
    public static final FurnitureHitBoxConfigFactory<CustomFurnitureHitbox> FACTORY = new Factory();
    public final float scale;
    public final Object entityType;
    public final List<Object> cachedValues = new ArrayList<>();
    public final float width;
    public final float height;

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

    private static class Factory implements FurnitureHitBoxConfigFactory<CustomFurnitureHitbox> {
        private static final String[] ENTITY_TYPE = new String[] {"entity_type", "entity-type"};
        private static final String[] CAN_USE_ITEM_ON = new String[] {"can_use_item_on", "can-use-item-on"};
        private static final String[] BLOCKS_BUILDING = new String[] {"blocks_building", "blocks-building"};
        private static final String[] CAN_BE_HIT_BY_PROJECTILE = new String[] {"can_be_hit_by_projectile", "can-be-hit-by-projectile"};

        @Override
        public CustomFurnitureHitboxConfig create(ConfigSection section) {
            ConfigValue typeValue = section.getNonNullValue(ENTITY_TYPE, ConfigConstants.ARGUMENT_IDENTIFIER);
            Object nmsEntityType = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ENTITY_TYPE, KeyUtils.toIdentifier(typeValue.getAsIdentifier()));
            if (nmsEntityType == null) {
                throw new KnownResourceException("resource.furniture.hitbox.custom.invalid_entity_type", typeValue.path(), typeValue.getAsString());
            }
            Object dimensions = EntityTypeProxy.INSTANCE.getDimensions(nmsEntityType);
            float width = EntityDimensionsProxy.INSTANCE.getWidth(dimensions);
            float height = EntityDimensionsProxy.INSTANCE.getHeight(dimensions);
            boolean fixed = EntityDimensionsProxy.INSTANCE.isFixed(dimensions);
            return new CustomFurnitureHitboxConfig(
                    section.getList("seats", SeatConfig::fromConfig).toArray(new SeatConfig[0]),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getBoolean(CAN_USE_ITEM_ON, true),
                    section.getBoolean(BLOCKS_BUILDING, true),
                    section.getBoolean(CAN_BE_HIT_BY_PROJECTILE, true),
                    width,
                    height,
                    fixed,
                    section.getFloat("scale", 1f),
                    nmsEntityType
            );
        }
    }
}
