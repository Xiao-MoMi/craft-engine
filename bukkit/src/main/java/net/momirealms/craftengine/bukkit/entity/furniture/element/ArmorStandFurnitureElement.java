package net.momirealms.craftengine.bukkit.entity.furniture.element;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class ArmorStandFurnitureElement extends AbstractFurnitureElement {
    public final ArmorStandFurnitureElementConfig config;
    public final Furniture furniture;
    public final Object cachedSpawnPacket;
    public final Object cachedDespawnPacket;
    public final Object cachedScalePacket;
    public final Object cachedTeamPacket;
    public final int entityId;
    public final UUID uuid = UUID.randomUUID();

    @Override
    public @NotNull Furniture furniture() {
        return this.furniture;
    }

    ArmorStandFurnitureElement(Furniture furniture, ArmorStandFurnitureElementConfig config) {
        super(config.predicate, config.hasCondition);
        this.config = config;
        this.furniture = furniture;
        this.entityId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                this.entityId, this.uuid, position.x, position.y, position.z,
                furniturePos.xRot, furniturePos.yRot, MEntityTypes.ARMOR_STAND, 0, Vec3Proxy.ZERO, furniturePos.yRot
        );
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(this.entityId));
        if (VersionHelper.isOrAbove1_20_5() && config.scale != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(MAttributeHolders.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale);
            this.cachedScalePacket = ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance(this.entityId, Collections.singletonList(attributeIns));
        } else {
            this.cachedScalePacket = null;
        }
        Object teamPacket = null;
        if (config.glowColor != null) {
            String teamName = BukkitTeamManager.instance().getTeamNameByColor(config.glowColor);
            if (teamName != null) {
                teamPacket = ClientboundSetPlayerTeamPacketProxy.INSTANCE.newInstance(teamName, 3, Optional.empty(), ImmutableList.of(this.uuid.toString()));
            }
        }
        this.cachedTeamPacket = teamPacket;
    }

    @Override
    public void showInternal(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadata.apply(player))), false);
        player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.furniture.dataAccessor.getColorSource()).getLiteralObject())
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
    public void refresh(Player player) {
        player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.furniture.dataAccessor.getColorSource()).getLiteralObject())
        )), false);
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
