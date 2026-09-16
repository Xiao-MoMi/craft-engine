package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.culling.ViewRangeCullable;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.element.TransformableFurnitureElement;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;

public final class BlockDisplayFurnitureElement extends AbstractConditionalFurnitureElement implements TransformableFurnitureElement, ViewRangeCullable {
    public final BlockDisplayFurnitureElementConfig config;
    public final Furniture furniture;
    public final WorldPosition position;
    public final int entityId;
    public final Object cachedDespawnPacket;
    public final Object cachedSpawnPacket;

    BlockDisplayFurnitureElement(Furniture furniture, BlockDisplayFurnitureElementConfig config, WorldPosition pos) {
        this(furniture, config, pos, EntityUtils.ENTITY_COUNTER.incrementAndGet());
    }

    BlockDisplayFurnitureElement(Furniture furniture, BlockDisplayFurnitureElementConfig config, WorldPosition pos, int entityId) {
        super(config.predicate);
        this.furniture = furniture;
        this.config = config;
        this.entityId = entityId;
        this.position = pos;
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(entityId)));
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, UUID.randomUUID(),
                this.position.x, this.position.y, this.position.z, this.position.xRot, this.position.yRot,
                EntityTypesProxy.BLOCK_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
    }

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPacket(ClientboundBundlePacketProxy.INSTANCE.newInstance(List.of(
                this.cachedSpawnPacket,
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, null, false))
        )), false);
    }

    @Override
    public void setCulled(Player player, boolean culled) {
        List<Object> values = new ArrayList<>(1);
        DisplayData.ViewRange.addEntityData(culled ? 0f : (float) (this.config.viewRange * player.displayEntityViewDistance()), values, true);
        player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, values), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void update(Player player) {
        player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, null, true)), false);
    }

    @Override
    public void gatherInteractableEntityId(IntConsumer collector) {
    }

    @Override
    public int entityId() {
        return this.entityId;
    }

    @Override
    public void update(Player player, TransformableFurnitureElement previous) {
        // 按玩家实际已显示的位置比较，允许跳过中间变体快照。
        if (!this.position.equals(previous.position())) {
            player.sendPacket(EntityUtils.createUpdatePosPacket(this.entityId, this.position.x, this.position.y, this.position.z, this.position.yRot, this.position.xRot, false), false);
        }
        this.update(player);
    }

    @Override
    public @NotNull WorldPosition position() {
        return this.position;
    }

}
