package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.furniture.ColliderConfig;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;

public final class HappyGhastFurnitureHitbox extends AbstractFurnitureHitBox {
    private final HappyGhastFurnitureHitboxConfig config;
    public final ColliderConfig colliderConfig;
    private final Object despawnPacket;
    public final FurnitureHitboxPart part;
    private final Vec3d pos;
    private final List<Object> packets;
    private final int entityId;
    private final float yaw;

    HappyGhastFurnitureHitbox(Furniture furniture, HappyGhastFurnitureHitboxConfig config) {
        super(furniture, config);
        this.config = config;
        WorldPosition position = furniture.position();
        this.pos = furniture.getRelativePosition(config.position);
        double bbSize = 4 * config.scale;
        AABB aabb = AABB.makeBoundingBox(this.pos, bbSize, bbSize);
        this.yaw = position.yRot;
        this.entityId = EntityUtils.ENTITY_COUNTER.incrementAndGet();
        this.packets = new ArrayList<>(3);
        this.packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, config.cachedValues));
        if (config.scale != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(AttributesProxy.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale);
            this.packets.add(ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance$0(this.entityId, Collections.singletonList(attributeIns)));
        }
        this.packets.add(EntityUtils.createUpdatePosPacket(this.entityId, this.pos.x, this.pos.y, this.pos.z, position.yRot, 0, false));
        this.colliderConfig = new ColliderConfig(aabb, config.colliderProperties);
        this.part = new FurnitureHitboxPart(this.entityId, aabb, this.pos, false);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(this.entityId)));
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
        return 1;
    }

    @Override
    public FurnitureHitboxPart part(int index) {
        return this.part;
    }

    @Override
    public void show(Player player) {
        List<Object> packets = new ArrayList<>();
        packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, UUID.randomUUID(), this.pos.x, player.y() - (this.config.scale * 4 + 16), this.pos.z, 0, this.yaw,
                EntityTypesProxy.HAPPY_GHAST, 0, Vec3Proxy.ZERO, 0
        ));
        packets.addAll(this.packets);
        player.sendPacket(ClientboundBundlePacketProxy.INSTANCE.newInstance(packets), false);
    }

    @Override
    public void showCulled(Player player) {
        this.show(player);
    }

    @Override
    public void cull(Player player) {
        // The entity is spawned with SharedFlags 0x20 (invisible).
    }

    @Override
    public void restore(Player player) {
        // The invisible entity remains spawned while culled.
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void collectInteractableEntityId(IntConsumer collector) {
        collector.accept(this.entityId);
    }

    @Override
    public HappyGhastFurnitureHitboxConfig config() {
        return this.config;
    }
}
