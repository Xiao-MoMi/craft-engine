package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.block.entity.render.element.AbstractConstantBlockEntityElement;
import net.momirealms.craftengine.core.entity.culling.ViewRangeCullable;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BlockDisplayBlockEntityElement extends AbstractConstantBlockEntityElement implements ViewRangeCullable {
    public final BlockDisplayBlockEntityElementConfig config;
    public final Object cachedSpawnPacket;
    public final Object cachedDespawnPacket;
    public final Object cachedUpdatePosPacket;
    public final int entityId;

    BlockDisplayBlockEntityElement(BlockDisplayBlockEntityElementConfig config, BlockPos pos) {
        this(config, pos, EntityUtils.ENTITY_COUNTER.incrementAndGet(), false);
    }

    BlockDisplayBlockEntityElement(BlockDisplayBlockEntityElementConfig config, BlockPos pos, int entityId, boolean posChanged) {
        super(config.predicate, config.hasCondition);
        Vector3f position = config.position();
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId, UUID.randomUUID(), pos.x() + (double) position.x, pos.y() + (double) position.y, pos.z() + (double) position.z,
                config.xRot(), config.yRot(), EntityTypesProxy.BLOCK_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.config = config;
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(entityId));
        this.entityId = entityId;
        this.cachedUpdatePosPacket = posChanged ? EntityUtils.createUpdatePosPacket(this.entityId, pos.x() + (double) position.x, pos.y() + (double) position.y, pos.z() + (double) position.z, config.yRot(), config.xRot(), false) : null;
    }

    @Override
    public void setCulled(Player player, boolean culled) {
        List<Object> values = new ArrayList<>(1);
        DisplayData.ViewRange.addEntityData(culled ? 0f : (float) (this.config.viewRange * player.displayEntityViewDistance()), values, true);
        player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, values), false);
    }

    @Override
    public void hide(@NotNull Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void showInternal(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player, false))), false);
    }

    @Override
    public void update(@NotNull Player player) {
        if (this.cachedUpdatePosPacket != null) {
            player.sendPackets(List.of(this.cachedUpdatePosPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player, true))), false);
        } else {
            player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player, true)), false);
        }
    }

    public int entityId() {
        return this.entityId;
    }

    @Override
    public boolean supportsTransform() {
        return true;
    }
}
