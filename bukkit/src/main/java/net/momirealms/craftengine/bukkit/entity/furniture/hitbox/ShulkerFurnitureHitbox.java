package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundUpdateAttributesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ShulkerFurnitureHitbox extends AbstractFurnitureHitBox {
    private final ShulkerFurnitureHitboxConfig config;
    private final List<FurnitureHitboxPart> parts;
    private final List<Collider> colliders;
    private final Object spawnPacket;
    private final Object despawnPacket;
    private final int[] entityIds;

    ShulkerFurnitureHitbox(Furniture furniture, ShulkerFurnitureHitboxConfig config) {
        super(furniture, config);
        this.config = config;
        this.entityIds = acquireEntityIds(EntityProxy.ENTITY_COUNTER::incrementAndGet);
        WorldPosition position = furniture.position();
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0f, (float) Math.toRadians(180 - position.yRot()), 0f).conjugate();
        Vector3f offset = conjugated.transform(new Vector3f(config.position()));
        double x = position.x();
        double y = position.y();
        double z = position.z();
        float yaw = position.yRot();
        double originalY = y + offset.y;
        double integerPart = Math.floor(originalY);
        double fractionalPart = originalY - integerPart;
        double processedY = (fractionalPart >= 0.5) ? integerPart + 1 : originalY;
        List<Object> packets = new ArrayList<>();
        List<Collider> colliders = new ArrayList<>();
        List<FurnitureHitboxPart> parts = new ArrayList<>();

        packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityIds[0], UUID.randomUUID(), x + offset.x, originalY, z - offset.z, 0, yaw,
                MEntityTypes.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        ));
        packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityIds[1], UUID.randomUUID(), x + offset.x, processedY, z - offset.z, 0, yaw,
                MEntityTypes.SHULKER, 0, Vec3Proxy.ZERO, 0
        ));
        packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[1], config.cachedShulkerValues()));
        packets.add(FastNMS.INSTANCE.constructor$ClientboundSetPassengersPacket(entityIds[0], entityIds[1]));

        // fix some special occasions
        if (originalY != processedY) {
            double deltaY = originalY - processedY;
            short ya = (short) (deltaY * 8192);
            try {
                packets.add(NetworkReflections.constructor$ClientboundMoveEntityPacket$Pos.newInstance(
                        entityIds[1], (short) 0, ya, (short) 0, true
                ));
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to construct ClientboundMoveEntityPacket$Pos", e);
            }
        }
        if (VersionHelper.isOrAbove1_20_5() && config.scale() != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(MAttributeHolders.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale());
            packets.add(ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance(this.entityIds[1], Collections.singletonList(attributeIns)));
        }
        config.spawner().accept(entityIds, position.world(), x, y, z, yaw, offset, packets::add, colliders::add, parts::add);
        this.parts = parts;
        this.colliders = colliders;
        this.spawnPacket = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(new IntArrayList(entityIds));
    }

    @Override
    public List<Collider> colliders() {
        return this.colliders;
    }

    @Override
    public List<FurnitureHitboxPart> parts() {
        return this.parts;
    }

    @Override
    public void collectVirtualEntityId(Consumer<Integer> collector) {
        for (int entityId : entityIds) {
            collector.accept(entityId);
        }
    }

    @Override
    public void show(Player player) {
        player.sendPacket(this.spawnPacket, false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public ShulkerFurnitureHitboxConfig config() {
        return this.config;
    }

    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        if (config.interactionEntity()) {
            if (config.direction().stepY() != 0) {
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
}
