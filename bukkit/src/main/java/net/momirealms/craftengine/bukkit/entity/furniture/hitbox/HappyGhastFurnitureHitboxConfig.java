package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.animal.happyghast.HappyGhastData;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.AbstractFurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigFactory;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class HappyGhastFurnitureHitboxConfig extends AbstractFurnitureHitBoxConfig<HappyGhastFurnitureHitbox> {
    public static final FurnitureHitBoxConfigFactory<HappyGhastFurnitureHitbox> FACTORY = new Factory();
    public final double scale;
    public final boolean hardCollision;
    public final List<Object> cachedValues = new ArrayList<>(3);

    private HappyGhastFurnitureHitboxConfig(SeatConfig[] seats,
                                           Vector3f position,
                                           boolean canUseItemOn,
                                           boolean blocksBuilding,
                                           boolean canBeHitByProjectile,
                                           double scale,
                                           boolean hardCollision) {
        super(seats, position, canUseItemOn, blocksBuilding, canBeHitByProjectile);
        this.scale = scale;
        this.hardCollision = hardCollision;
        HappyGhastData.StaysStill.addEntityDataIfNotDefaultValue(hardCollision, this.cachedValues);
        HappyGhastData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, this.cachedValues); // NO AI
        HappyGhastData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedValues); // Invisible
    }

    public double scale() {
        return this.scale;
    }

    public boolean hardCollision() {
        return this.hardCollision;
    }

    public List<Object> cachedValues() {
        return this.cachedValues;
    }

    @Override
    public HappyGhastFurnitureHitbox create(Furniture furniture) {
        return new HappyGhastFurnitureHitbox(furniture, this);
    }

    @Override
    public void prepareBoundingBox(WorldPosition targetPos, Consumer<AABB> aabbConsumer, boolean ignoreBlocksBuilding) {
        if (this.blocksBuilding || ignoreBlocksBuilding) {
            Vec3d relativePosition = Furniture.getRelativePosition(targetPos, this.position);
            aabbConsumer.accept(AABB.makeBoundingBox(relativePosition, 4 * this.scale, 4 * this.scale));
        }
    }

    private static class Factory implements FurnitureHitBoxConfigFactory<HappyGhastFurnitureHitbox> {
        private static final String[] CAN_USE_ITEM_ON = new String[] {"can_use_item_on", "can-use-item-on"};
        private static final String[] BLOCKS_BUILDING = new String[] {"blocks_building", "blocks-building"};
        private static final String[] CAN_BE_HIT_BY_PROJECTILE = new String[] {"can_be_hit_by_projectile", "can-be-hit-by-projectile"};
        private static final String[] HARD_COLLISION = new String[] {"hard_collision", "hard-collision"};

        @Override
        public FurnitureHitBoxConfig<HappyGhastFurnitureHitbox> create(ConfigSection section) {
            return new HappyGhastFurnitureHitboxConfig(
                    section.getList("seats", SeatConfig::fromConfig).toArray(new SeatConfig[0]),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getBoolean(CAN_USE_ITEM_ON, true),
                    section.getBoolean(BLOCKS_BUILDING, true),
                    section.getBoolean(CAN_BE_HIT_BY_PROJECTILE, true),
                    section.getDouble("scale", 1d),
                    section.getBoolean(HARD_COLLISION, true)
            );
        }
    }
}
