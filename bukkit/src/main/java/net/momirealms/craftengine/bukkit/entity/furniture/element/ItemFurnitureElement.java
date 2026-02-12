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
        this.entityId1 = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
        this.entityId2 = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.cachedSpawnPacket1 = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId1, UUID.randomUUID(), position.x, position.y, position.z,
                0, 0, MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0
        );
        this.cachedSpawnPacket2 = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId2, UUID.randomUUID(), position.x, position.y, position.z,
                0, 0, MEntityTypes.ITEM, 0, CoreReflections.instance$Vec3$Zero, 0
        );
        this.cachedRidePacket = FastNMS.INSTANCE.constructor$ClientboundSetPassengersPacket(entityId1, entityId2);
        this.despawnPacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(MiscUtils.init(new IntArrayList(),
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
                FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId2, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource())
        )), false);
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.despawnPacket, false);
    }

    @Override
    public void refresh(Player player) {
        player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId2, this.config.metadata.apply(player, this.furniture.dataAccessor.getColorSource())), false);
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
