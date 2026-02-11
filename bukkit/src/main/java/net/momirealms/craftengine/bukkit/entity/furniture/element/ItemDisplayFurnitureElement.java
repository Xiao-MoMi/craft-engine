package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ItemDisplayFurnitureElement extends AbstractFurnitureElement {
    public final ItemDisplayFurnitureElementConfig config;
    public final Furniture furniture;
    public final WorldPosition position;
    public final int entityId;
    public final Object despawnPacket;
    public final UUID uuid = UUID.randomUUID();

    ItemDisplayFurnitureElement(Furniture furniture, ItemDisplayFurnitureElementConfig config) {
        super(config.predicate, config.hasCondition);
        this.config = config;
        this.furniture = furniture;
        this.entityId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.position = new WorldPosition(furniturePos.world, position.x, position.y, position.z, furniturePos.xRot, furniturePos.yRot);
        this.despawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(entityId)));
    }

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPacket(ClientboundBundlePacketProxy.INSTANCE.newInstance(List.of(
                ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                        this.entityId, this.uuid,
                        this.position.x, this.position.y, this.position.z, 0, this.position.yRot,
                        MEntityTypes.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
                ),
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource()))
        )), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void refresh(Player player) {
        player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource())), false);
    }

    @Override
    public int[] virtualEntityIds() {
        return new int[] {this.entityId};
    }

    @Override
    public void collectVirtualEntityId(Consumer<Integer> collector) {
        collector.accept(this.entityId);
    }
}
