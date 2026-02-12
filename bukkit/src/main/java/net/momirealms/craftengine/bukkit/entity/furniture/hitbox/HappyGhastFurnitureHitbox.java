package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class HappyGhastFurnitureHitbox extends AbstractFurnitureHitBox {
    private final HappyGhastFurnitureHitboxConfig config;
    private final Collider collider;
    private final Object despawnPacket;
    private final FurnitureHitboxPart part;
    private final Vec3d pos;
    private final List<Object> packets;
    private final int entityId;
    private final float yaw;

    HappyGhastFurnitureHitbox(Furniture furniture, HappyGhastFurnitureHitboxConfig config) {
        super(furniture, config);
        this.config = config;
        WorldPosition position = furniture.position();
        this.pos = Furniture.getRelativePosition(position, config.position());
        double bbSize = 4 * config.scale();
        AABB aabb = AABB.makeBoundingBox(this.pos, bbSize, bbSize);
        this.yaw = position.yRot;
        this.entityId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        this.packets = new ArrayList<>(3);
        this.packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, config.cachedValues()));
        if (config.scale() != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(MAttributeHolders.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale());
            this.packets.add(ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance(this.entityId, Collections.singletonList(attributeIns)));
        }
        this.packets.add(EntityUtils.createUpdatePosPacket(this.entityId, this.pos.x, this.pos.y, this.pos.z, position.yRot, 0, false));
        this.collider = createCollider(furniture.world(), this.pos, aabb, config.hardCollision(), config.blocksBuilding(), config.canBeHitByProjectile());
        this.part = new FurnitureHitboxPart(this.entityId, aabb, this.pos, false);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(new IntArrayList() {{ add(entityId); }});
    }

    @Override
    public List<Collider> colliders() {
        return List.of(this.collider);
    }

    @Override
    public List<FurnitureHitboxPart> parts() {
        return List.of(this.part);
    }

    @Override
    public void show(Player player) {
        List<Object> packets = new ArrayList<>();
        packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, UUID.randomUUID(), this.pos.x, player.y() - (this.config.scale() * 4 + 16), this.pos.z, 0, this.yaw,
                MEntityTypes.HAPPY_GHAST, 0, Vec3Proxy.ZERO, 0
        ));
        packets.addAll(this.packets);
        player.sendPacket(ClientboundBundlePacketProxy.INSTANCE.newInstance(packets), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void collectVirtualEntityId(Consumer<Integer> collector) {
        collector.accept(this.entityId);
    }

    @Override
    public HappyGhastFurnitureHitboxConfig config() {
        return this.config;
    }
}
