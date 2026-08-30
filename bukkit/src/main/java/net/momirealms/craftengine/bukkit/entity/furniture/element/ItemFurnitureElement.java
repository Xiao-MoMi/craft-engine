package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.furniture.data.ItemPatch;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ItemFurnitureElement extends AbstractConditionalFurnitureElement {
    public final ItemFurnitureElementConfig config;
    public final Furniture furniture;
    public final FurnitureDataResolver<ItemPatch> itemPatch;
    public final Vec3d position;
    public final int entityId1;
    public final int entityId2;
    public final Object despawnPacket;
    public final Object cachedSpawnPacket1;
    public final Object cachedSpawnPacket2;
    public final Object cachedRidePacket;
    public final Object cachedUpdatePosPacket;

    ItemFurnitureElement(Furniture furniture, ItemFurnitureElementConfig config, Vec3d pos) {
        this(furniture, config, pos, EntityUtils.ENTITY_COUNTER.incrementAndGet(), EntityUtils.ENTITY_COUNTER.incrementAndGet(), false);
    }

    ItemFurnitureElement(Furniture furniture, ItemFurnitureElementConfig config, Vec3d pos, int entityId1, int entityId2, boolean positionChanged) {
        super(config.predicate, config.hasCondition);
        this.furniture = furniture;
        this.itemPatch = config.createItemPatch(furniture);
        this.config = config;
        this.entityId1 = entityId1;
        this.entityId2 = entityId2;
        this.position = pos;
        this.cachedSpawnPacket1 = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId1, UUID.randomUUID(), position.x, position.y, position.z,
                0, 0, EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedSpawnPacket2 = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId2, UUID.randomUUID(), position.x, position.y, position.z,
                0, 0, EntityTypesProxy.ITEM, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedRidePacket = PacketUtils.createClientboundSetPassengersPacket(entityId1, entityId2);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(),
                a -> {
                    a.add(entityId1);
                    a.add(entityId2);
                }
        ));
        this.cachedUpdatePosPacket = positionChanged ? EntityUtils.createUpdatePosPacket(this.entityId1, position.x, position.y, position.z, 0, 0, false) : null;
    }

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPackets(List.of(
                this.cachedSpawnPacket1,
                this.cachedSpawnPacket2,
                this.cachedRidePacket,
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadata.apply(player, this.itemPatch)
        )), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void update(Player player) {
        if (this.cachedUpdatePosPacket != null) {
            player.sendPackets(List.of(this.cachedUpdatePosPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadata.apply(player, this.itemPatch))), false);
        } else {
            player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadata.apply(player, this.itemPatch)), false);
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
