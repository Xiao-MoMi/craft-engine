package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.furniture.ColliderConfig;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class ShulkerFurnitureHitbox extends AbstractFurnitureHitBox {
    private final ShulkerFurnitureHitboxConfig config;
    private final FurnitureHitboxPart[] parts;
    public final ColliderConfig colliderConfig;
    private final Object spawnPacket;
    private final Object despawnPacket;
    private final Object culledSpawnPacket;
    private final Object interactionSpawnPacket;
    private final Object interactionDespawnPacket;
    private final int[] entityIds;

    ShulkerFurnitureHitbox(Furniture furniture, ShulkerFurnitureHitboxConfig config) {
        super(furniture, config);
        this.config = config;
        this.entityIds = acquireEntityIds(EntityUtils.ENTITY_COUNTER::incrementAndGet);
        WorldPosition position = furniture.position();
        Vector3f offset = furniture.placement().rotateOffset(config.position);
        double x = position.x();
        double y = position.y();
        double z = position.z();
        float yaw = position.yRot();
        double originalY = y + offset.y;
        double integerPart = Math.floor(originalY);
        double fractionalPart = originalY - integerPart;
        double processedY = (fractionalPart >= 0.5) ? integerPart + 1 : originalY;
        List<Object> packets = new ArrayList<>();
        this.parts = new FurnitureHitboxPart[this.entityIds.length - 1];

        packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityIds[0], UUID.randomUUID(), x + offset.x, originalY, z - offset.z, 0, yaw,
                EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        ));
        // Older clients can reuse another display's shadow for this empty carrier (MC-276123).
        packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[0],
                List.of(BaseEntityData.SharedFlags.createEntityData((byte) 0x20))));
        packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityIds[1], UUID.randomUUID(), x + offset.x, processedY, z - offset.z, 0, yaw,
                EntityTypesProxy.SHULKER, 0, Vec3Proxy.ZERO, 0
        ));
        packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[1], config.cachedShulkerValues));
        packets.add(PacketUtils.createClientboundSetPassengersPacket(entityIds[0], entityIds[1]));

        // fix some special occasions
        if (originalY != processedY) {
            double deltaY = originalY - processedY;
            short ya = (short) (deltaY * 8192);
            packets.add(ClientboundMoveEntityPacketProxy.PosProxy.INSTANCE.newInstance(
                    this.entityIds[1], (short) 0, ya, (short) 0, true
            ));
        }
        if (VersionHelper.isOrAbove1_20_5 && config.scale != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(AttributesProxy.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale);
            packets.add(ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance$0(this.entityIds[1], Collections.singletonList(attributeIns)));
        }
        this.colliderConfig = config.spawner.create(entityIds, x, y, z, yaw, offset, packets, this.parts);

        // The directional spawner appends one spawn/data pair per interaction entity.
        int interactionCount = this.entityIds.length - 2;
        int interactionStart = packets.size() - interactionCount * 2;
        this.culledSpawnPacket = ClientboundBundlePacketProxy.INSTANCE.newInstance(List.copyOf(packets.subList(0, interactionStart)));
        this.interactionSpawnPacket = interactionCount == 0 ? null : ClientboundBundlePacketProxy.INSTANCE.newInstance(List.copyOf(packets.subList(interactionStart, packets.size())));
        this.interactionDespawnPacket = interactionCount == 0 ? null : ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(new IntArrayList(java.util.Arrays.copyOfRange(this.entityIds, 2, this.entityIds.length)));
        this.spawnPacket = ClientboundBundlePacketProxy.INSTANCE.newInstance(packets);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(new IntArrayList(this.entityIds));
    }

    @Override
    public void collectCullingBounds(Consumer<AABB> consumer) {
        consumer.accept(this.colliderConfig.bounds);
    }

    @Override
    public int colliderConfigCount() {
        return 1;
    }

    @Override
    public ColliderConfig colliderConfig(int index) {
        return this.colliderConfig;
    }

    @Override
    public int partCount() {
        return this.parts.length;
    }

    @Override
    public FurnitureHitboxPart part(int index) {
        return this.parts[index];
    }

    @Override
    public void collectInteractableEntityId(IntConsumer collector) {
        for (int entityId : entityIds) {
            collector.accept(entityId);
        }
    }

    @Override
    public void show(Player player) {
        player.sendPacket(this.spawnPacket, false);
    }

    @Override
    public void showCulled(Player player) {
        player.sendPacket(this.config.invisible ? this.spawnPacket : this.culledSpawnPacket, false);
    }

    @Override
    public void cull(Player player) {
        if (!this.config.invisible && this.interactionDespawnPacket != null) {
            player.sendPacket(this.interactionDespawnPacket, false);
        }
    }

    @Override
    public void restore(Player player) {
        if (!this.config.invisible && this.interactionSpawnPacket != null) {
            player.sendPacket(this.interactionSpawnPacket, false);
        }
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public ShulkerFurnitureHitboxConfig config() {
        return this.config;
    }

    public int[] acquireEntityIds(IntSupplier entityIdSupplier) {
        if (config.interactionEntity) {
            if (config.direction.stepY() != 0) {
                // 展示实体                 // 潜影贝               // 交互实体
                return new int[] {entityIdSupplier.getAsInt(), entityIdSupplier.getAsInt(), entityIdSupplier.getAsInt()};
            } else {
                // 展示实体                 // 潜影贝               // 交互实体1              // 交互实体2
                return new int[] {entityIdSupplier.getAsInt(), entityIdSupplier.getAsInt(), entityIdSupplier.getAsInt(), entityIdSupplier.getAsInt()};
            }
        } else {
            // 展示实体                 // 潜影贝
            return new int[] {entityIdSupplier.getAsInt(), entityIdSupplier.getAsInt()};
        }
    }
}
