package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ItemFurnitureElement extends AbstractFurnitureElement {
    public final ItemFurnitureElementConfig config;
    public final Furniture furniture;
    public final int entityId1;
    public final int entityId2;
    public final Object despawnPacket;
    public final Object cachedSpawnPacket1;
    public final Object cachedSpawnPacket2;
    public final Object cachedRidePacket;

    ItemFurnitureElement(Furniture furniture, ItemFurnitureElementConfig config) {
        super(config.predicate, config.hasCondition);
        this.furniture = furniture;
        this.config = config;
        this.entityId1 = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        this.entityId2 = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.cachedSpawnPacket1 = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId1, UUID.randomUUID(), position.x, position.y, position.z,
                0, 0, EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedSpawnPacket2 = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId2, UUID.randomUUID(), position.x, position.y, position.z,
                0, 0, EntityTypeProxy.ITEM, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedRidePacket = PacketUtils.createClientboundSetPassengersPacket(entityId1, entityId2);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(),
                a -> {
                    a.add(entityId1);
                    a.add(entityId2);
                }
        ));
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
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource())
        )), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void refresh(Player player) {
        player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource())), false);
    }

    @Override
    public int[] virtualEntityIds() {
        return new int[] {this.entityId1, this.entityId2};
    }

    @Override
    public void collectVirtualEntityId(Consumer<Integer> collector) {
        collector.accept(this.entityId1);
        collector.accept(this.entityId2);
    }
}
