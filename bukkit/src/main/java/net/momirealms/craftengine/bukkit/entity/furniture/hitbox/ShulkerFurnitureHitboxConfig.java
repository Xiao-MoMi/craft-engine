package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.InteractionData;
import net.momirealms.craftengine.bukkit.entity.data.monster.ShulkerData;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.entity.furniture.ColliderConfig;
import net.momirealms.craftengine.core.entity.furniture.ColliderProperties;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.AbstractFurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigFactory;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ShulkerFurnitureHitboxConfig extends AbstractFurnitureHitBoxConfig<ShulkerFurnitureHitbox> {
    public final ColliderProperties colliderProperties;
    public static final FurnitureHitBoxConfigFactory<ShulkerFurnitureHitbox> FACTORY = new Factory();
    public final float scale;
    public final byte peek;
    public final boolean interactive;
    public final boolean interactionEntity;
    public final Direction direction;
    public final DirectionalShulkerSpawner spawner;
    public final boolean invisible;
    public final List<Object> cachedShulkerValues = new ArrayList<>(6);
    public final AABBCreator aabbCreator;

    private ShulkerFurnitureHitboxConfig(SeatConfig[] seats,
                                        Vector3f position,
                                        boolean canUseItemOn,
                                        boolean blocksBuilding,
                                        boolean canBeHitByProjectile,
                                        float scale,
                                        byte peek,
                                        boolean interactive,
                                        boolean interactionEntity,
                                        boolean invisible,
                                        Direction direction) {
        super(seats, position, canUseItemOn, blocksBuilding, canBeHitByProjectile);
        this.colliderProperties = ColliderProperties.of(true, blocksBuilding, canBeHitByProjectile);
        this.scale = scale;
        this.peek = peek;
        this.invisible = invisible;
        this.interactive = interactive;
        this.interactionEntity = interactionEntity;
        this.direction = direction;

        ShulkerData.RawPeekAmount.addEntityDataIfNotDefaultValue(peek, this.cachedShulkerValues);
        ShulkerData.Color.addEntityDataIfNotDefaultValue((byte) 0, this.cachedShulkerValues);
        ShulkerData.NoGravity.addEntityDataIfNotDefaultValue(true, this.cachedShulkerValues);
        ShulkerData.Silent.addEntityDataIfNotDefaultValue(true, this.cachedShulkerValues);
        ShulkerData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, this.cachedShulkerValues); // NO AI
        ShulkerData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedShulkerValues); // Invisible

        List<Object> cachedInteractionValues = new ArrayList<>();
        if (invisible) {
            InteractionData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, cachedInteractionValues);
        }
        float shulkerHeight = (getPhysicalPeek(peek * 0.01F) + 1) * scale;
        if (direction == Direction.UP) {
            InteractionData.Height.addEntityDataIfNotDefaultValue(shulkerHeight + 0.01f, cachedInteractionValues);
            InteractionData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionData.Response.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            List<Object> interactionValues = List.copyOf(cachedInteractionValues);
            this.spawner = (entityIds, x, y, z, yaw, offset, packets, parts) -> {
                ColliderConfig collider = this.createColliderConfig(Direction.UP, offset, x, y, z, entityIds[1], parts);
                if (interactionEntity) {
                    packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f, z - offset.z, 0, yaw,
                            EntityTypesProxy.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[2], interactionValues));
                    Vec3d vec3d = new Vec3d(x + offset.x, y + offset.y, z - offset.z);
                    parts[1] = new FurnitureHitboxPart(entityIds[2], AABB.makeBoundingBox(vec3d, scale, shulkerHeight), vec3d, interactive);
                }
                return collider;
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> createAABB(Direction.UP, offset, x, y, z);
        } else if (direction == Direction.DOWN) {
            InteractionData.Height.addEntityDataIfNotDefaultValue(shulkerHeight + 0.01f, cachedInteractionValues);
            InteractionData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionData.Response.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            List<Object> interactionValues = List.copyOf(cachedInteractionValues);
            this.spawner = (entityIds, x, y, z, yaw, offset, packets, parts) -> {
                ColliderConfig collider = this.createColliderConfig(Direction.DOWN, offset, x, y, z, entityIds[1], parts);
                packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[1], List.of(ShulkerData.AttachFace.createEntityDataIfNotDefaultValue(DirectionProxy.UP))));
                if (interactionEntity) {
                    packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f - shulkerHeight + scale, z - offset.z, 0, yaw,
                            EntityTypesProxy.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[2], interactionValues));
                    Vec3d vec3d = new Vec3d(x + offset.x, y + offset.y - shulkerHeight + scale, z - offset.z);
                    parts[1] = new FurnitureHitboxPart(entityIds[2], AABB.makeBoundingBox(vec3d, scale, shulkerHeight), vec3d, interactive);
                }
                return collider;
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> createAABB(Direction.DOWN, offset, x, y, z);
        } else {
            InteractionData.Height.addEntityDataIfNotDefaultValue(scale + 0.01f, cachedInteractionValues);
            InteractionData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionData.Response.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            List<Object> interactionValues = List.copyOf(cachedInteractionValues);
            this.spawner = (entityIds, x, y, z, yaw, offset, packets, parts) -> {
                Direction shulkerAnchor = getOriginalDirection(direction, Direction.fromYaw(yaw));
                Direction shulkerDirection = shulkerAnchor.opposite();
                ColliderConfig collider = this.createColliderConfig(shulkerDirection, offset, x, y, z, entityIds[1], parts);
                packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[1], List.of(ShulkerData.AttachFace.createEntityDataIfNotDefaultValue(DirectionUtils.toNMSDirection(shulkerAnchor)))));
                if (interactionEntity) {
                    // first interaction
                    packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f, z - offset.z, 0, yaw,
                            EntityTypesProxy.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[2], interactionValues));
                    // second interaction
                    double distance = shulkerHeight - scale;
                    packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                            entityIds[3], UUID.randomUUID(), x + offset.x + shulkerDirection.stepX() * distance, y + offset.y - 0.005f, z - offset.z + shulkerDirection.stepZ() * distance, 0, yaw,
                            EntityTypesProxy.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[3], interactionValues));
                    Vec3d vec3d1 = new Vec3d(x + offset.x, y + offset.y, z - offset.z);
                    Vec3d vec3d2 = new Vec3d(x + offset.x + shulkerDirection.stepX() * distance, y + offset.y, z - offset.z + shulkerDirection.stepZ() * distance);
                    parts[1] = new FurnitureHitboxPart(entityIds[2], AABB.makeBoundingBox(vec3d1, scale, scale), vec3d1, interactive);
                    parts[2] = new FurnitureHitboxPart(entityIds[3], AABB.makeBoundingBox(vec3d2, scale, scale), vec3d2, interactive);
                }
                return collider;
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> {
                Direction shulkerAnchor = getOriginalDirection(direction, Direction.fromYaw(yaw));
                Direction shulkerDirection = shulkerAnchor.opposite();
                return createAABB(shulkerDirection, offset, x, y, z);
            };
        }
    }

    public static float getPhysicalPeek(float peek) {
        return 0.5F - MiscUtils.sin((0.5F + peek) * 3.1415927F) * 0.5F;
    }

    @Override
    public ColliderProperties colliderProperties() {
        return this.colliderProperties;
    }

    @Override
    public void prepareBoundingBox(WorldPosition targetPos, Consumer<AABB> aabbConsumer, boolean ignoreBlocksBuilding) {
        if (this.blocksBuilding || ignoreBlocksBuilding) {
            Vector3f offset = Furniture.rotateHitboxOffset(targetPos.yRot, this.position);
            aabbConsumer.accept(this.aabbCreator.create(targetPos.x, targetPos.y, targetPos.z, targetPos.yRot, offset));
        }
    }

    public float scale() {
        return this.scale;
    }

    public byte peek() {
        return this.peek;
    }

    public boolean interactive() {
        return this.interactive;
    }

    public boolean interactionEntity() {
        return this.interactionEntity;
    }

    public Direction direction() {
        return this.direction;
    }

    public DirectionalShulkerSpawner spawner() {
        return this.spawner;
    }

    public List<Object> cachedShulkerValues() {
        return this.cachedShulkerValues;
    }

    @Override
    public ShulkerFurnitureHitbox create(Furniture furniture) {
        return new ShulkerFurnitureHitbox(furniture, this);
    }

    @FunctionalInterface
    public interface AABBCreator {

        AABB create(double x, double y, double z, float yaw, Vector3f offset);
    }

    @FunctionalInterface
    public interface DirectionalShulkerSpawner {

        ColliderConfig create(int[] entityIds,
                    double x,
                    double y,
                    double z,
                    float yaw,
                    Vector3f offset,
                    List<Object> packets,
                    FurnitureHitboxPart[] parts);
    }

    public ColliderConfig createColliderConfig(Direction direction,
                                   Vector3f offset, double x, double y, double z,
                                   int entityId,
                                   FurnitureHitboxPart[] parts) {
        AABB ceAABB = createAABB(direction, offset, x, y, z);
        parts[0] = new FurnitureHitboxPart(entityId, ceAABB, new Vec3d(x, y, z), false);
        return new ColliderConfig(ceAABB, this.colliderProperties);
    }

    public AABB createAABB(Direction direction, Vector3f relativePos, double x, double y, double z) {
        float peek = getPhysicalPeek(this.peek * 0.01F);
        double x1 = -this.scale * 0.5;
        double y1 = 0.0;
        double z1 = -this.scale * 0.5;
        double x2 = this.scale * 0.5;
        double y2 = this.scale;
        double z2 = this.scale * 0.5;

        double dx = (double) direction.stepX() * peek * (double) this.scale;
        if (dx > 0) {
            x2 += dx;
        } else if (dx < 0) {
            x1 += dx;
        }
        double dy = (double) direction.stepY() * peek * (double) this.scale;
        if (dy > 0) {
            y2 += dy;
        } else if (dy < 0) {
            y1 += dy;
        }
        double dz = (double) direction.stepZ() * peek * (double) this.scale;
        if (dz > 0) {
            z2 += dz;
        } else if (dz < 0) {
            z1 += dz;
        }
        double minX = x + x1 + relativePos.x;
        double maxX = x + x2 + relativePos.x;
        double minY = y + y1 + relativePos.y;
        double maxY = y + y2 + relativePos.y;
        double minZ = z + z1 - relativePos.z;
        double maxZ = z + z2 - relativePos.z;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static Direction getOriginalDirection(Direction newDirection, Direction oldDirection) {
        switch (newDirection) {
            case NORTH -> {
                return switch (oldDirection) {
                    case NORTH -> Direction.NORTH;
                    case SOUTH -> Direction.SOUTH;
                    case WEST -> Direction.EAST;
                    case EAST -> Direction.WEST;
                    default -> throw new IllegalStateException("Unexpected value: " + oldDirection);
                };
            }
            case SOUTH -> {
                return switch (oldDirection) {
                    case SOUTH -> Direction.NORTH;
                    case WEST -> Direction.WEST;
                    case EAST -> Direction.EAST;
                    case NORTH -> Direction.SOUTH;
                    default -> throw new IllegalStateException("Unexpected value: " + oldDirection);
                };
            }
            case WEST -> {
                return switch (oldDirection) {
                    case SOUTH -> Direction.EAST;
                    case WEST -> Direction.NORTH;
                    case EAST -> Direction.SOUTH;
                    case NORTH -> Direction.WEST;
                    default -> throw new IllegalStateException("Unexpected value: " + oldDirection);
                };
            }
            case EAST -> {
                return switch (oldDirection) {
                    case SOUTH -> Direction.WEST;
                    case WEST -> Direction.SOUTH;
                    case EAST -> Direction.NORTH;
                    case NORTH -> Direction.EAST;
                    default -> throw new IllegalStateException("Unexpected value: " + oldDirection);
                };
            }
            default -> throw new IllegalStateException("Unexpected value: " + newDirection);
        }
    }

    private static class Factory implements FurnitureHitBoxConfigFactory<ShulkerFurnitureHitbox> {
        private static final String[] CAN_USE_ITEM_ON = ConfigKeys.of("can_use_item_on");
        private static final String[] BLOCKS_BUILDING = ConfigKeys.of("blocks_building");
        private static final String[] CAN_BE_HIT_BY_PROJECTILE = ConfigKeys.of("can_be_hit_by_projectile");
        private static final String[] INTERACTION_ENTITY = ConfigKeys.of("interaction_entity");

        @Override
        public ShulkerFurnitureHitboxConfig create(ConfigSection section) {
            return new ShulkerFurnitureHitboxConfig(
                    section.getList("seats", SeatConfig::fromConfig).toArray(new SeatConfig[0]),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getBoolean(CAN_USE_ITEM_ON, true),
                    section.getBoolean(BLOCKS_BUILDING, true),
                    section.getBoolean(CAN_BE_HIT_BY_PROJECTILE, true),
                    section.getFloat("scale", 1f),
                    (byte) section.getInt("peek"),
                    section.getBoolean("interactive", true),
                    section.getBoolean(INTERACTION_ENTITY, true),
                    section.getBoolean("invisible"),
                    section.getEnum("direction", Direction.class, Direction.UP)
            );
        }
    }
}
