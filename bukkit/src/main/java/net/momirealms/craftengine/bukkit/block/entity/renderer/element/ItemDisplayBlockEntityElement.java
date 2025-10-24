package net.momirealms.craftengine.bukkit.block.entity.renderer.element;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class ItemDisplayBlockEntityElement implements BlockEntityElement {
    private final ItemDisplayBlockEntityElementConfig config;
    private final Object cachedSpawnPacket;
    private final Object cachedDespawnPacket;
    private final int entityId;

    public ItemDisplayBlockEntityElement(ItemDisplayBlockEntityElementConfig config, BlockPos pos) {
        int entityId = CoreReflections.Instance.entity$ENTITY_COUNTER.incrementAndGet();
        Vector3f position = config.position();
        this.cachedSpawnPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(), pos.x() + position.x, pos.y() + position.y, pos.z() + position.z,
                config.xRot(), config.yRot(), MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.Instance.vec3$Zero, 0
        );
        this.config = config;
        this.cachedDespawnPacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(IntList.of(entityId));
        this.entityId = entityId;
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void show(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket, FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, this.config.metadataValues(player))), true);
    }
}
