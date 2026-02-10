package net.momirealms.craftengine.bukkit.block.entity.renderer.element;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class ItemBlockEntityElement implements BlockEntityElement {
    public final ItemBlockEntityElementConfig config;
    public final Object cachedSpawnPacket1;
    public final Object cachedSpawnPacket2;
    public final Object cachedRidePacket;
    public final Object cachedDespawnPacket;
    public final Object cachedUpdatePosPacket;
    public final int entityId1;
    public final int entityId2;

    public ItemBlockEntityElement(ItemBlockEntityElementConfig config, BlockPos pos) {
        this(config, pos, EntityProxy.ENTITY_COUNTER.incrementAndGet(), EntityProxy.ENTITY_COUNTER.incrementAndGet(), false);
    }

    public ItemBlockEntityElement(ItemBlockEntityElementConfig config, BlockPos pos, int entityId1, int entityId2, boolean posChanged) {
        this.config = config;
        Vector3f position = config.position();
        this.cachedSpawnPacket1 = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId1, UUID.randomUUID(), pos.x() + position.x, pos.y() + position.y, pos.z() + position.z,
                0, 0, MEntityTypes.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedSpawnPacket2 = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId2, UUID.randomUUID(), pos.x() + position.x, pos.y() + position.y, pos.z() + position.z,
                0, 0, MEntityTypes.ITEM, 0, Vec3Proxy.ZERO, 0
        );
        this.cachedRidePacket = FastNMS.INSTANCE.constructor$ClientboundSetPassengersPacket(entityId1, entityId2);
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(entityId1, entityId2));
        this.entityId1 = entityId1;
        this.entityId2 = entityId2;
        this.cachedUpdatePosPacket = posChanged ? EntityUtils.createUpdatePosPacket(this.entityId1, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z, 0, 0, false) : null;
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void show(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket1, this.cachedSpawnPacket2, this.cachedRidePacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadataValues(player))), false);
    }

    @Override
    public void transform(Player player) {
        if (this.cachedUpdatePosPacket != null) {
            player.sendPackets(List.of(this.cachedUpdatePosPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadataValues(player))), false);
        } else {
            player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId2, this.config.metadataValues(player)), false);
        }
    }
}
