package net.momirealms.craftengine.bukkit.entity.furniture.element;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.furniture.data.ItemPatch;
import net.momirealms.craftengine.core.entity.furniture.element.TransformableFurnitureElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.score.TeamManagerImpl;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntConsumer;

public final class ArmorStandFurnitureElement extends AbstractConditionalFurnitureElement implements TransformableFurnitureElement {
    public final ArmorStandFurnitureElementConfig config;
    public final Furniture furniture;
    public final WorldPosition position;
    public final FurnitureDataResolver<ItemPatch> itemPatch;
    public final Object cachedSpawnPacket;
    public final Object cachedDespawnPacket;
    public final Object cachedScalePacket;
    public final Object cachedTeamPacket;
    public final int entityId;

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    ArmorStandFurnitureElement(Furniture furniture, ArmorStandFurnitureElementConfig config, WorldPosition pos) {
        this(furniture, config, pos, EntityUtils.ENTITY_COUNTER.incrementAndGet());
    }

    ArmorStandFurnitureElement(Furniture furniture, ArmorStandFurnitureElementConfig config, WorldPosition pos, int entityId) {
        super(config.predicate);
        this.config = config;
        this.furniture = furniture;
        this.itemPatch = config.createItemPatch(furniture);
        this.entityId = entityId;
        this.position = pos;
        UUID uuid = UUID.randomUUID();
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, uuid, position.x, position.y, position.z,
                this.position.xRot, this.position.yRot, EntityTypesProxy.ARMOR_STAND, 0, Vec3Proxy.ZERO, this.position.yRot
        );
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(this.entityId));
        if (VersionHelper.isOrAbove1_20_5 && config.scale != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(AttributesProxy.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale);
            this.cachedScalePacket = ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance$0(this.entityId, Collections.singletonList(attributeIns));
        } else {
            this.cachedScalePacket = null;
        }
        Object teamPacket = null;
        if (config.glowColor != null) {
            String teamName = TeamManagerImpl.instance().getTeamNameByColor(config.glowColor);
            if (teamName != null) {
                teamPacket = ClientboundSetPlayerTeamPacketProxy.INSTANCE.newInstance(teamName, 3, Optional.empty(), ImmutableList.of(uuid.toString()));
            }
        }
        this.cachedTeamPacket = teamPacket;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player))), false);
        player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.itemPatch).minecraftItem())
        )), false);
        if (this.cachedScalePacket != null) {
            player.sendPacket(this.cachedScalePacket, false);
        }
        if (this.cachedTeamPacket != null) {
            player.sendPacket(this.cachedTeamPacket, false);
        }
    }

    @Override
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void update(Player player) {
        player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.itemPatch).minecraftItem())
        )), false);
    }

    @Override
    public void gatherInteractableEntityId(IntConsumer collector) {
        collector.accept(this.entityId);
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
