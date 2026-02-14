package net.momirealms.craftengine.bukkit.block.entity.renderer;

import com.google.common.cache.Cache;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.block.entity.ItemFrameBlockEntity;
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.Vec3i;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundMapItemDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.MapItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.saveddata.maps.MapItemSavedDataProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Vector3f;

import java.util.UUID;

public class DynamicItemFrameRenderer implements DynamicBlockEntityRenderer {
    public final ItemFrameBlockEntity blockEntity;
    public final Object cachedSpawnPacket;
    public final Object cachedDespawnPacket;
    public final int entityId;

    public DynamicItemFrameRenderer(ItemFrameBlockEntity blockEntity, BlockPos pos) {
        this.entityId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        this.blockEntity = blockEntity;
        Vector3f position = this.blockEntity.behavior.position;
        Direction direction = blockEntity.blockState().get(blockEntity.behavior.directionProperty);
        Vec3i axisZ = direction.vector();
        Vec3i axisX = direction.axis().isVertical() ? Direction.EAST.vector() : direction.clockWise().vector();
        double worldX, worldY, worldZ;
        if (direction.axis().isVertical()) {
            worldX = pos.x + position.x * axisX.x;
            worldY = pos.y + position.z * axisZ.y;
            worldZ = pos.z + position.y * (direction == Direction.UP ? -1 : 1);
        } else {
            worldX = pos.x + (axisX.x * position.x) + (axisZ.x * position.z);
            worldY = pos.y + position.y;
            worldZ = pos.z + (axisX.z * position.x) + (axisZ.z * position.z);
        }
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, UUID.randomUUID(), worldX, worldY, worldZ, 0, 0,
                this.blockEntity.behavior.glow ? EntityTypeProxy.GLOW_ITEM_FRAME : EntityTypeProxy.ITEM_FRAME,
                direction.ordinal(), Vec3Proxy.ZERO, 0
        );
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(entityId));
    }

    @Override
    public void show(Player player) {
        player.sendPacket(this.cachedSpawnPacket, false);
        update(player);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void update(Player player) {
        player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.blockEntity.cacheMetadata()), false);
        if (this.blockEntity.behavior.renderMapItem) {
            updateMapItem(player);
        }
    }

    private void updateMapItem(Player player) {
        if (player.isFakePlayer()) return;
        Object mapId = this.blockEntity.mapId();
        if (mapId == null) return;
        CEWorld world = this.blockEntity.world;
        if (world == null) return;
        Object savedData = this.blockEntity.mapItemSavedData();
        if (savedData == null) {
            if (VersionHelper.isOrAbove1_20_5()) {
                savedData = MapItemProxy.INSTANCE.getSavedData$0(mapId, world.world.serverWorld());
            } else {
                savedData = MapItemProxy.INSTANCE.getSavedData$1((Integer) mapId, world.world.serverWorld());
            }
            if (savedData == null) return;
            this.blockEntity.setMapItemSavedData(savedData);
        }
        try {
            Cache<Object, Boolean> receivedMapData = player.receivedMapData();
            Object received = receivedMapData.getIfPresent(savedData);
            if (received != null) return; // 节约带宽静态渲染
            receivedMapData.put(savedData, Boolean.TRUE); // 存入用于标记的单例对象
            byte[] colors = MapItemSavedDataProxy.INSTANCE.getColors(savedData);
            Object patch = MapItemSavedDataProxy.MapPatchProxy.INSTANCE.newInstance(0, 0, 128, 128, colors);
            byte scale = MapItemSavedDataProxy.INSTANCE.getScale(savedData);
            boolean locked = MapItemSavedDataProxy.INSTANCE.getLocked(savedData);
            Object packet;
            if (VersionHelper.isOrAbove1_20_5()) {
                packet = ClientboundMapItemDataPacketProxy.INSTANCE.newInstance$0(mapId, scale, locked, null, patch);
            } else {
                packet = ClientboundMapItemDataPacketProxy.INSTANCE.newInstance$1((Integer) mapId, scale, locked, null, patch);
            }
            player.sendPacket(packet, false);
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Cannot update map item for player " + player.name(), e);
        }
    }
}
