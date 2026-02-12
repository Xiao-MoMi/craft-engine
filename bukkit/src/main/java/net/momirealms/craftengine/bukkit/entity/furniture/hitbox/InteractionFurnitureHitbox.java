package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class InteractionFurnitureHitbox extends AbstractFurnitureHitBox {
    private final InteractionFurnitureHitboxConfig config;
    private final Collider collider;
    private final Object spawnPacket;
    private final Object despawnPacket;
    private final FurnitureHitboxPart part;
    private final int entityId;

    InteractionFurnitureHitbox(Furniture furniture, InteractionFurnitureHitboxConfig config) {
        super(furniture, config);
        this.config = config;
        WorldPosition position = furniture.position();
        Vec3d pos = Furniture.getRelativePosition(position, config.position());
        AABB aabb = AABB.makeBoundingBox(pos, config.size().x, config.size().y);
        this.collider = createCollider(furniture.world(), pos, aabb, false, config.blocksBuilding(), config.canBeHitByProjectile());
        int interactionId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        this.spawnPacket = ClientboundBundlePacketProxy.INSTANCE.newInstance(List.of(
                ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                        interactionId, UUID.randomUUID(), pos.x, pos.y, pos.z, 0, position.yRot,
                        MEntityTypes.INTERACTION, 0, Vec3Proxy.ZERO, 0
                ),
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(interactionId, config.cachedValues())
        ));
        this.part = new FurnitureHitboxPart(interactionId, aabb, pos, config.responsive());
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(interactionId)));
        this.entityId = interactionId;
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
    public InteractionFurnitureHitboxConfig config() {
        return this.config;
    }

    @Override
    public void collectVirtualEntityId(Consumer<Integer> collector) {
        collector.accept(this.entityId);
    }

    @Override
    public void show(Player player) {
        player.sendPacket(this.spawnPacket, false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }
}
