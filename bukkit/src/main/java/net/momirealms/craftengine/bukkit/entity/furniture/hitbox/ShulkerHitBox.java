package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.data.InteractionEntityData;
import net.momirealms.craftengine.bukkit.entity.data.ShulkerData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitCollider;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShulkerHitBox extends AbstractHitBox {
    public static final Factory FACTORY = new Factory();
    // 1.20.6+
    private final float scale;
    private final byte peek;
    private final boolean interactive;
    private final boolean interactionEntity;
    private final Direction direction;
    private final List<Object> cachedShulkerValues = new ArrayList<>();
    private final DirectionalShulkerSpawner spawner;
    private final AABBCreator aabbCreator;

    public ShulkerHitBox(Seat[] seats, Vector3f position, Direction direction, float scale, byte peek, boolean interactionEntity, boolean interactive, boolean canUseOn, boolean blocksBuilding, boolean canBeHitByProjectile) {
        super(seats, position, canUseOn, blocksBuilding, canBeHitByProjectile);
        this.direction = direction;
        this.scale = scale;
        this.peek = peek;
        this.interactive = interactive;
        this.interactionEntity = interactionEntity;

        ShulkerData.Peek.addEntityDataIfNotDefaultValue(peek, this.cachedShulkerValues);
        ShulkerData.Color.addEntityDataIfNotDefaultValue((byte) 0, this.cachedShulkerValues);
        ShulkerData.NoGravity.addEntityDataIfNotDefaultValue(true, this.cachedShulkerValues);
        ShulkerData.Silent.addEntityDataIfNotDefaultValue(true, this.cachedShulkerValues);
        ShulkerData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, this.cachedShulkerValues); // NO AI
        ShulkerData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, this.cachedShulkerValues); // Invisible

        float shulkerHeight = (getPhysicalPeek(peek * 0.01F) + 1) * scale;
        List<Object> cachedInteractionValues = new ArrayList<>();
        if (this.direction == Direction.UP) {
            InteractionEntityData.Height.addEntityDataIfNotDefaultValue(shulkerHeight + 0.01f, cachedInteractionValues);
            InteractionEntityData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            this.spawner = (entityIds, world, x, y, z, yaw, offset, packets, collider, aabb) -> {
                collider.accept(this.createCollider(Direction.UP, world, offset, x, y, z, entityIds[1], aabb));
                if (interactionEntity) {
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f, z - offset.z, 0, yaw,
                            MEntityTypes.INTERACTION, 0, CoreReflections.instance$Vec3$Zero, 0
                    ), true);
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[2], List.copyOf(cachedInteractionValues)), true);
                    if (canUseOn) {
                        aabb.accept(entityIds[2], AABB.fromInteraction(new Vec3d(x + offset.x, y + offset.y, z - offset.z), scale, shulkerHeight));
                    }
                }
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> createAABB(Direction.UP, offset, x, y, z);
        } else if (this.direction == Direction.DOWN) {
            InteractionEntityData.Height.addEntityDataIfNotDefaultValue(shulkerHeight + 0.01f, cachedInteractionValues);
            InteractionEntityData.Width.addEntityDataIfNotDefaultValue(scale + 0.005f, cachedInteractionValues);
            InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(interactive, cachedInteractionValues);
            this.spawner = (entityIds, world, x, y, z, yaw, offset, packets, collider, aabb) -> {
                collider.accept(this.createCollider(Direction.DOWN, world, offset, x, y, z, entityIds[1], aabb));
                packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[1], List.of(ShulkerData.AttachFace.createEntityDataIfNotDefaultValue(CoreReflections.instance$Direction$UP))), false);
                if (interactionEntity) {
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f - shulkerHeight + scale, z - offset.z, 0, yaw,
                            MEntityTypes.INTERACTION, 0, CoreReflections.instance$Vec3$Zero, 0
                    ), true);
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[2], List.copyOf(cachedInteractionValues)), true);
                    if (canUseOn) {
                        aabb.accept(entityIds[2], AABB.fromInteraction(new Vec3d(x + offset.x, y + offset.y - shulkerHeight + scale, z - offset.z), scale, shulkerHeight));
                    }
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
                packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[1], List.of(ShulkerData.AttachFace.createEntityDataIfNotDefaultValue(DirectionUtils.toNMSDirection(shulkerAnchor)))), false);
                if (interactionEntity) {
                    // first interaction
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[2], UUID.randomUUID(), x + offset.x, y + offset.y - 0.005f, z - offset.z, 0, yaw,
                            MEntityTypes.INTERACTION, 0, CoreReflections.instance$Vec3$Zero, 0
                    ), true);
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[2], List.copyOf(cachedInteractionValues)), true);
                    // second interaction
                    double distance = shulkerHeight - scale;
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityIds[3], UUID.randomUUID(), x + offset.x + shulkerDirection.stepX() * distance, y + offset.y - 0.005f, z - offset.z + shulkerDirection.stepZ() * distance, 0, yaw,
                            MEntityTypes.INTERACTION, 0, CoreReflections.instance$Vec3$Zero, 0
                    ), true);
                    packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[3], List.copyOf(cachedInteractionValues)), true);
                    if (canUseOn) {
                        aabb.accept(entityIds[2], AABB.fromInteraction(new Vec3d(x + offset.x, y + offset.y, z - offset.z), scale, scale));
                        aabb.accept(entityIds[3], AABB.fromInteraction(new Vec3d(x + offset.x + shulkerDirection.stepX() * distance, y + offset.y, z - offset.z + shulkerDirection.stepZ() * distance), scale, scale));
                    }
                }
            };
            this.aabbCreator = (x, y, z, yaw, offset) -> {
                Direction shulkerAnchor = getOriginalDirection(direction, Direction.fromYaw(yaw));
                Direction shulkerDirection = shulkerAnchor.opposite();
                return createAABB(shulkerDirection, offset, x, y, z);
            };
        }
    }

    public Collider createCollider(Direction direction, World world, Vector3f offset, double x, double y, double z, int entityId, BiConsumer<Integer, AABB> aabb) {
        AABB ceAABB = createAABB(direction, offset, x, y, z);
        Object level = world.serverWorld();
        Object nmsAABB = FastNMS.INSTANCE.constructor$AABB(ceAABB.minX, ceAABB.minY, ceAABB.minZ, ceAABB.maxX, ceAABB.maxY, ceAABB.maxZ);
        aabb.accept(entityId, ceAABB);
        return new BukkitCollider(level, nmsAABB, x, y, z, this.canBeHitByProjectile(), true, this.blocksBuilding());
    }

    public AABB createAABB(Direction direction, Vector3f offset, double x, double y, double z) {
        float peek = getPhysicalPeek(this.peek() * 0.01F);
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
        double minX = x + x1 + offset.x();
        double maxX = x + x2 + offset.x();
        double minY = y + y1 + offset.y();
        double maxY = y + y2 + offset.y();
        double minZ = z + z1 - offset.z();
        double maxZ = z + z2 - offset.z();
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static float getPhysicalPeek(float peek) {
        return 0.5F - MCUtils.sin((0.5F + peek) * 3.1415927F) * 0.5F;
    }

    public boolean interactionEntity() {
        return interactionEntity;
    }

    public boolean interactive() {
        return interactive;
    }

    public Direction direction() {
        return direction;
    }

    public byte peek() {
        return peek;
    }

    public float scale() {
        return scale;
    }

    @Override
    public Key type() {
        return HitBoxTypes.SHULKER;
    }

    @Override
    public void initPacketsAndColliders(int[] entityIds, WorldPosition position, Quaternionf conjugated, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, BiConsumer<Integer, AABB> aabb) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        try {
            double x = position.x();
            double y = position.y();
            double z = position.z();
            float yaw = position.xRot();
            double originalY = y + offset.y;
            double integerPart = Math.floor(originalY);
            double fractionalPart = originalY - integerPart;
            double processedY = (fractionalPart >= 0.5) ? integerPart + 1 : originalY;
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                    entityIds[0], UUID.randomUUID(), x + offset.x, originalY, z - offset.z, 0, yaw,
                    MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0
            ), false);
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                    entityIds[1], UUID.randomUUID(), x + offset.x, processedY, z - offset.z, 0, yaw,
                    MEntityTypes.SHULKER, 0, CoreReflections.instance$Vec3$Zero, 0
            ), false);
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityIds[1], List.copyOf(this.cachedShulkerValues)), false);
            // add passengers
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetPassengersPacket(entityIds[0], entityIds[1]), false);
            // fix some special occasions
            if (originalY != processedY) {
                double deltaY = originalY - processedY;
                short ya = (short) (deltaY * 8192);
                packets.accept(NetworkReflections.constructor$ClientboundMoveEntityPacket$Pos.newInstance(
                        entityIds[1], (short) 0, ya, (short) 0, true
                ), false);
            }
            // set shulker scale
            if (VersionHelper.isOrAbove1_20_5() && this.scale != 1) {
                Object attributeInstance = CoreReflections.constructor$AttributeInstance.newInstance(MAttributeHolders.SCALE, (Consumer<?>) (o) -> {});
                CoreReflections.method$AttributeInstance$setBaseValue.invoke(attributeInstance, this.scale);
                packets.accept(NetworkReflections.constructor$ClientboundUpdateAttributesPacket0.newInstance(entityIds[1], Collections.singletonList(attributeInstance)), false);
            }
            this.spawner.accept(entityIds, position.world(), x, y, z, yaw, offset, packets, collider, aabb);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to construct shulker hitbox spawn packet", e);
        }
    }

    @Override
    public void initShapeForPlacement(double x, double y, double z, float yaw, Quaternionf conjugated, Consumer<AABB> aabbs) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        aabbs.accept(this.aabbCreator.create(x, y, z, yaw, offset));
    }

    @FunctionalInterface
    interface DirectionalShulkerSpawner {

        void accept(int[] entityIds, World world, double x, double y, double z, float yaw, Vector3f offset, BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, BiConsumer<Integer, AABB> aabb);
    }

    @FunctionalInterface
    interface AABBCreator {

        AABB create(double x, double y, double z, float yaw, Vector3f offset);
    }

    @Override
    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        if (this.interactionEntity) {
            if (this.direction.stepY() != 0) {
                                // 展示实体                 // 潜影贝               // 交互实体
                return new int[] {entityIdSupplier.get(), entityIdSupplier.get(), entityIdSupplier.get()};
            } else {
                                // 展示实体                 // 潜影贝               // 交互实体1              // 交互实体2
                return new int[] {entityIdSupplier.get(), entityIdSupplier.get(), entityIdSupplier.get(), entityIdSupplier.get()};
            }
        } else {
                            // 展示实体                 // 潜影贝
            return new int[] {entityIdSupplier.get(), entityIdSupplier.get()};
        }
    }

    public static class Factory implements HitBoxFactory {

        @Override
        public HitBox create(Map<String, Object> arguments) {
            Vector3f position = MiscUtils.getAsVector3f(arguments.getOrDefault("position", "0"), "position");
            float scale = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("scale", "1"), "scale");
            byte peek = (byte) ResourceConfigUtils.getAsInt(arguments.getOrDefault("peek", 0), "peek");
            Direction directionEnum = Optional.ofNullable(arguments.get("direction")).map(it -> Direction.valueOf(it.toString().toUpperCase(Locale.ENGLISH))).orElse(Direction.UP);
            boolean interactive = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("interactive", true), "interactive");
            boolean interactionEntity = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("interaction-entity", true), "interaction-entity");
            boolean canUseItemOn = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-use-item-on", true), "can-use-item-on");
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-hit-by-projectile", true), "can-be-hit-by-projectile");
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blocks-building", true), "blocks-building");
            return new ShulkerHitBox(
                    HitBoxFactory.getSeats(arguments),
                    position, directionEnum,
                    scale, peek, interactionEntity, interactive, canUseItemOn, blocksBuilding, canBeHitByProjectile
            );
        }
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
}
