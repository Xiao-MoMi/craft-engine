package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.InteractionData;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.AbstractFurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigFactory;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class InteractionFurnitureHitboxConfig extends AbstractFurnitureHitBoxConfig<InteractionFurnitureHitbox> {
    public static final FurnitureHitBoxConfigFactory<InteractionFurnitureHitbox> FACTORY = new Factory();
    public static final InteractionFurnitureHitboxConfig DEFAULT = new InteractionFurnitureHitboxConfig();
    public final Vector3f size;
    public final boolean responsive;
    public final boolean invisible;
    public final List<Object> cachedValues = new ArrayList<>(4);

    private InteractionFurnitureHitboxConfig(SeatConfig[] seats,
                                            Vector3f position,
                                            boolean canUseItemOn,
                                            boolean blocksBuilding,
                                            boolean canBeHitByProjectile,
                                            boolean invisible,
                                            Vector3f size,
                                            boolean interactive) {
        super(seats, position, canUseItemOn, blocksBuilding, canBeHitByProjectile);
        this.size = size;
        this.responsive = interactive;
        this.invisible = invisible;
        InteractionData.Height.addEntityDataIfNotDefaultValue(size.y, this.cachedValues);
        InteractionData.Width.addEntityDataIfNotDefaultValue(size.x, this.cachedValues);
        InteractionData.Response.addEntityDataIfNotDefaultValue(interactive, this.cachedValues);
        if (invisible) {
            BaseEntityData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedValues);
        }
    }

    private InteractionFurnitureHitboxConfig() {
        super(new SeatConfig[0], new Vector3f(), false, false, false);
        this.size = new Vector3f(1);
        this.responsive = true;
        this.invisible = false;
    }

    public Vector3f size() {
        return this.size;
    }

    public boolean responsive() {
        return this.responsive;
    }

    public boolean invisible() {
        return this.invisible;
    }

    public List<Object> cachedValues() {
        return this.cachedValues;
    }

    @Override
    public void prepareBoundingBox(WorldPosition targetPos, Consumer<AABB> aabbConsumer, boolean ignoreBlocksBuilding) {
        if (this.blocksBuilding || ignoreBlocksBuilding) {
            Vec3d relativePosition = Furniture.getRelativePosition(targetPos, this.position);
            aabbConsumer.accept(AABB.makeBoundingBox(relativePosition, this.size.x, this.size.y));
        }
    }

    @Override
    public InteractionFurnitureHitbox create(Furniture furniture) {
        return new InteractionFurnitureHitbox(furniture, this);
    }

    private static class Factory implements FurnitureHitBoxConfigFactory<InteractionFurnitureHitbox> {
        private static final String[] CAN_USE_ITEM_ON = new String[] {"can_use_item_on", "can-use-item-on"};
        private static final String[] BLOCKS_BUILDING = new String[] {"blocks_building", "blocks-building"};
        private static final String[] CAN_BE_HIT_BY_PROJECTILE = new String[] {"can_be_hit_by_projectile", "can-be-hit-by-projectile"};

        @Override
        public InteractionFurnitureHitboxConfig create(ConfigSection section) {
            float width;
            float height;
            ConfigValue optionalScale = section.getValue("scale");
            if (optionalScale != null) {
                String scaleString = optionalScale.getAsString();
                String[] splitScale = scaleString.split(",");
                if (splitScale.length == 1) {
                    width = optionalScale.getAsFloat();
                    height = optionalScale.getAsFloat();
                } else if (splitScale.length == 2) {
                    ConfigValue[] split = optionalScale.splitValuesRestrict(",", 2);
                    width = split[0].getAsFloat();
                    height = split[1].getAsFloat();
                } else {
                    ConfigValue[] split = optionalScale.splitValues(",");
                    width = split[0].getAsFloat();
                    height = split[1].getAsFloat();
                }
            } else {
                width = section.getFloat("width", 1f);
                height = section.getFloat("height", 1f);
            }
            return new InteractionFurnitureHitboxConfig(
                    section.getList("seats", SeatConfig::fromConfig).toArray(new SeatConfig[0]),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getBoolean(CAN_USE_ITEM_ON, true),
                    section.getBoolean(BLOCKS_BUILDING, true),
                    section.getBoolean(CAN_BE_HIT_BY_PROJECTILE, true),
                    section.getBoolean("invisible"),
                    new Vector3f(width, height, width),
                    section.getBoolean("interactive", true)
            );
        }
    }
}
