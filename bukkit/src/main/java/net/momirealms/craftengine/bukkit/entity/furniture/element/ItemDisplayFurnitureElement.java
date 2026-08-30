package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.furniture.data.ItemPatch;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ItemDisplayFurnitureElement extends AbstractConditionalFurnitureElement {
    public final ItemDisplayFurnitureElementConfig config;
    public final Furniture furniture;
    public final FurnitureDataResolver<ItemPatch> itemPatch;
    public final WorldPosition position;
    public final int entityId;
    public final Object cachedDespawnPacket;
    public final Object cachedSpawnPacket;
    public final Object cachedUpdatePosPacket;
    public final UUID uuid = UUID.randomUUID();

    ItemDisplayFurnitureElement(Furniture furniture, ItemDisplayFurnitureElementConfig config, WorldPosition pos) {
        this(furniture, config, pos, EntityUtils.ENTITY_COUNTER.incrementAndGet(), false);
    }

    ItemDisplayFurnitureElement(Furniture furniture, ItemDisplayFurnitureElementConfig config, WorldPosition pos, int entityId, boolean positionChanged) {
        super(config.predicate, config.hasCondition);
        this.config = config;
        this.furniture = furniture;
        this.entityId = entityId;
        this.itemPatch = config.createItemPatch(furniture);
        this.position = pos;
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(entityId)));
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, this.uuid,
                this.position.x, this.position.y, this.position.z, this.position.xRot, this.position.yRot,
                EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedUpdatePosPacket = positionChanged ? EntityUtils.createUpdatePosPacket(this.entityId, this.position.x, this.position.y, this.position.z, this.position.yRot, this.position.xRot, false) : null;
    }

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPacket(ClientboundBundlePacketProxy.INSTANCE.newInstance(List.of(
                this.cachedSpawnPacket,
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, this.itemPatch, false))
        )), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void update(Player player) {
        if (this.cachedUpdatePosPacket != null) {
            player.sendPackets(List.of(this.cachedUpdatePosPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, this.itemPatch, true))), false);
        } else {
            player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, this.itemPatch, true)), false);
        }
    }

    @Override
    public void gatherInteractableEntityId(Consumer<Integer> collector) {
    }

    @Override
    public boolean supportsTransform() {
        return true;
    }
}
