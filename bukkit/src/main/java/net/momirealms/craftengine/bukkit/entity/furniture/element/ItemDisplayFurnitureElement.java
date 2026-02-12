package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
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
        this.entityId = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.position = new WorldPosition(furniturePos.world, position.x, position.y, position.z, furniturePos.xRot, furniturePos.yRot);
        this.despawnPacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(MiscUtils.init(new IntArrayList(), a -> a.add(entityId)));
    }

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundBundlePacket(List.of(
                FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                        this.entityId, this.uuid,
                        this.position.x, this.position.y, this.position.z, 0, this.position.yRot,
                        MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0
                ),
                FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource()))
        )), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void refresh(Player player) {
        player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource())), false);
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
