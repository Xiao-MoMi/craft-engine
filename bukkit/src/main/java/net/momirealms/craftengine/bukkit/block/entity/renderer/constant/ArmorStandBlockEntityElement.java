package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.block.entity.render.element.AbstractConstantBlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSource;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ArmorStandBlockEntityElement extends AbstractConstantBlockEntityElement {
    public final ArmorStandBlockEntityElementConfig config;
    public final Object cachedSpawnPacket;
    public final Object cachedDespawnPacket;
    public final Object cachedUpdatePosPacket;
    public final Object cachedScalePacket;
    public final Object cachedTeamPacket;
    public final int entityId;
    public final UUID uuid = UUID.randomUUID();
    @Nullable
    public BlockEntityTintSource tintSource;

    public ArmorStandBlockEntityElement(ArmorStandBlockEntityElementConfig config, BlockPos pos, BlockEntityTintSource tintSource) {
        this(config, pos, tintSource, EntityProxy.ENTITY_COUNTER.incrementAndGet(), false);
    }

    public ArmorStandBlockEntityElement(ArmorStandBlockEntityElementConfig config, BlockPos pos, @Nullable BlockEntityTintSource tintSource, int entityId, boolean posChanged) {
        super(config.predicate, config.hasCondition);
        Vector3f position = config.position();
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId, this.uuid, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z,
                config.xRot(), config.yRot(), EntityTypeProxy.ARMOR_STAND, 0, Vec3Proxy.ZERO, config.yRot()
        );
        this.config = config;
        this.tintSource = tintSource;
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(entityId));
        this.entityId = entityId;
        this.cachedUpdatePosPacket = posChanged ? EntityUtils.createUpdatePosPacket(this.entityId, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z, config.yRot(), config.xRot(), false) : null;
        if (VersionHelper.isOrAbove1_20_5 && config.scale() != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(AttributesProxy.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale());
            this.cachedScalePacket = ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance$0(entityId, Collections.singletonList(attributeIns));
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
    public void hide(@NotNull Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void showInternal(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player))), false);
        player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.tintSource).minecraftItem())
        )), false);
        if (this.cachedDespawnPacket != null) {
            player.sendPacket(this.cachedDespawnPacket, false);
        }
        if (this.cachedTeamPacket != null) {
            player.sendPacket(this.cachedTeamPacket, false);
        }
    }

    @Override
    public void update(@NotNull Player player) {
        if (this.cachedUpdatePosPacket != null) {
            player.sendPackets(List.of(
                    this.cachedUpdatePosPacket,
                    ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player)),
                    ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                            Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.tintSource).minecraftItem())
                    ))
            ), false);
        } else {
            player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player)), false);
            player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                    Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player, this.tintSource).minecraftItem())
            )), false);
        }
    }

    @Override
    public boolean supportsTransform() {
        return true;
    }
}
