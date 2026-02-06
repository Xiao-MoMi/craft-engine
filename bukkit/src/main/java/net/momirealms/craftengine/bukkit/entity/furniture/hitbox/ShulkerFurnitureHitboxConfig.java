package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.InteractionEntityData;
import net.momirealms.craftengine.bukkit.entity.data.ShulkerData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitCollider;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.AbstractFurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigFactory;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ShulkerFurnitureHitboxConfig extends AbstractFurnitureHitBoxConfig<ShulkerFurnitureHitbox> {
    public static final Factory FACTORY = new Factory();
    private final float scale;
    private final byte peek;
    private final boolean interactive;
    private final boolean interactionEntity;
    private final Direction direction;
    private final DirectionalShulkerSpawner spawner;
    private final List<Object> cachedShulkerValues = new ArrayList<>(6);
    private final AABBCreator aabbCreator;

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
        this.scale = scale;
        this.peek = peek;
        this.interactive = interactive;
        this.interactionEntity = interactionEntity;
        this.direction = direction;

        ShulkerData.Peek.addEntityDataIfNotDefaultValue(peek, this.cachedShulkerValues);
        ShulkerData.Color.addEntityDataIfNotDefaultValue((byte) 0, this.cachedShulkerValues);
        ShulkerData.NoGravity.addEntityDataIfNotDefaultValue(true, this.cachedShulkerValues);
        ShulkerData.Silent.addEntityDataIfNotDefaultValue(true, this.cachedShulkerValues);
        ShulkerData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, this.cachedShulkerValues); // NO AI
        ShulkerData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedShulkerValues); // Invisible

        List<Object> cachedInteractionValues = new ArrayList<>();
        if (invisible) {
            InteractionEntityData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, cachedInteractionValues);
        }
        float shulkerHeight = (getPhysicalPeek(peek * 0.01F) + 1) * scale;
        if (direction == Direction.UP) {
            InteractionEntityData.Height.addEntityDataIfNotDefaultValue(shulkerHeight + 0.01f, cachedInteractionValues);
            InteractionEntityData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            this.spawner = (entityIds, world, x, y, z, yaw, offset, packets, collider, aabb) -> {
                collider.accept(this.createCollider(Direction.UP, world, offset, x, y, z, entityIds[1], aabb));
                if (interactionEntity) {
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f, z - offset.z, 0, yaw,
                            MEntityTypes.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[2], List.copyOf(cachedInteractionValues)));
                    Vec3d vec3d = new Vec3d(x + offset.x, y + offset.y, z - offset.z);
                    aabb.accept(new FurnitureHitboxPart(entityIds[2], AABB.makeBoundingBox(vec3d, scale, shulkerHeight), vec3d, interactive));
                }
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> createAABB(Direction.UP, offset, x, y, z);
        } else if (direction == Direction.DOWN) {
            InteractionEntityData.Height.addEntityDataIfNotDefaultValue(shulkerHeight + 0.01f, cachedInteractionValues);
            InteractionEntityData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            this.spawner = (entityIds, world, x, y, z, yaw, offset, packets, collider, aabb) -> {
                collider.accept(this.createCollider(Direction.DOWN, world, offset, x, y, z, entityIds[1], aabb));
                packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[1], List.of(ShulkerData.AttachFace.createEntityDataIfNotDefaultValue(DirectionProxy.UP))));
                if (interactionEntity) {
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f - shulkerHeight + scale, z - offset.z, 0, yaw,
                            MEntityTypes.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[2], List.copyOf(cachedInteractionValues)));
                    Vec3d vec3d = new Vec3d(x + offset.x, y + offset.y - shulkerHeight + scale, z - offset.z);
                    aabb.accept(new FurnitureHitboxPart(entityIds[2], AABB.makeBoundingBox(vec3d, scale, shulkerHeight), vec3d, interactive));
                }
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> createAABB(Direction.DOWN, offset, x, y, z);
        } else {
            InteractionEntityData.Height.addEntityDataIfNotDefaultValue(scale + 0.01f, cachedInteractionValues);
            InteractionEntityData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            this.spawner = (entityIds, world, x, y, z, yaw, offset, packets, collider, aabb) -> {
                Direction shulkerAnchor = getOriginalDirection(direction, Direction.fromYaw(yaw));
                Direction shulkerDirection = shulkerAnchor.opposite();
                collider.accept(this.createCollider(shulkerDirection, world, offset, x, y, z, entityIds[1], aabb));
                packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[1], List.of(ShulkerData.AttachFace.createEntityDataIfNotDefaultValue(DirectionUtils.toNMSDirection(shulkerAnchor)))));
                if (interactionEntity) {
                    // first interaction
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f, z - offset.z, 0, yaw,
                            MEntityTypes.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[2], List.copyOf(cachedInteractionValues)));
                    // second interaction
                    double distance = shulkerHeight - scale;
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[3], UUID.randomUUID(), x + offset.x + shulkerDirection.stepX() * distance, y + offset.y - 0.005f, z - offset.z + shulkerDirection.stepZ() * distance, 0, yaw,
                            MEntityTypes.INTERACTION, 0, Vec3Proxy.ZERO, 0
                    ));
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[3], List.copyOf(cachedInteractionValues)));
                    Vec3d vec3d1 = new Vec3d(x + offset.x, y + offset.y, z - offset.z);
                    Vec3d vec3d2 = new Vec3d(x + offset.x + shulkerDirection.stepX() * distance, y + offset.y, z - offset.z + shulkerDirection.stepZ() * distance);
                    aabb.accept(new FurnitureHitboxPart(entityIds[2], AABB.makeBoundingBox(vec3d1, scale, scale), vec3d1, interactive));
                    aabb.accept(new FurnitureHitboxPart(entityIds[3], AABB.makeBoundingBox(vec3d2, scale, scale), vec3d2, interactive));
                }
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
    public void prepareBoundingBox(WorldPosition targetPos, Consumer<AABB> aabbConsumer, boolean ignoreBlocksBuilding) {
        if (this.blocksBuilding || ignoreBlocksBuilding) {
            Quaternionf conjugated = QuaternionUtils.toQuaternionf(0f, (float) Math.toRadians(180 - targetPos.yRot()), 0f).conjugate();
            Vector3f offset = conjugated.transform(new Vector3f(position()));
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

        void accept(int[] entityIds,
                    World world,
                    double x,
                    double y,
                    double z,
                    float yaw,
                    Vector3f offset,
                    Consumer<Object> packets,
                    Consumer<Collider> collider,
                    Consumer<FurnitureHitboxPart> aabb);
    }

    public Collider createCollider(Direction direction, World world,
                                   Vector3f offset, double x, double y, double z,
                                   int entityId,
                                   Consumer<FurnitureHitboxPart> aabb) {
        AABB ceAABB = createAABB(direction, offset, x, y, z);
        Object level = world.serverWorld();
        Object nmsAABB = FastNMS.INSTANCE.constructor$AABB(ceAABB.minX, ceAABB.minY, ceAABB.minZ, ceAABB.maxX, ceAABB.maxY, ceAABB.maxZ);
        aabb.accept(new FurnitureHitboxPart(entityId, ceAABB, new Vec3d(x, y, z), false));
        return new BukkitCollider(level, nmsAABB, x, y, z, this.canBeHitByProjectile(), true, this.blocksBuilding());
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
        double minX = x + x1 + relativePos.x();
        double maxX = x + x2 + relativePos.x();
        double minY = y + y1 + relativePos.y();
        double maxY = y + y2 + relativePos.y();
        double minZ = z + z1 - relativePos.z();
        double maxZ = z + z2 - relativePos.z();
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

    public static class Factory implements FurnitureHitBoxConfigFactory<ShulkerFurnitureHitbox> {

        @Override
        public ShulkerFurnitureHitboxConfig create(Map<String, Object> arguments) {
            Vector3f position = ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", 0), "position");
            float scale = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("scale", 1), "scale");
            byte peek = (byte) ResourceConfigUtils.getAsInt(arguments.getOrDefault("peek", 0), "peek");
            Direction directionEnum = ResourceConfigUtils.getAsEnum(arguments.get("direction"), Direction.class, Direction.UP);
            boolean interactive = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("interactive", true), "interactive");
            boolean interactionEntity = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("interaction-entity", true), "interaction-entity");
            boolean canUseItemOn = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-use-item-on", true), "can-use-item-on");
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-hit-by-projectile", true), "can-be-hit-by-projectile");
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blocks-building", true), "blocks-building");
            boolean invisible = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("invisible", false), "invisible");
            return new ShulkerFurnitureHitboxConfig(
                    SeatConfig.fromObj(arguments.get("seats")),
                    position,
                    canUseItemOn, blocksBuilding, canBeHitByProjectile,
                    scale, peek, interactive, interactionEntity, invisible, directionEnum
            );
        }
    }
}
