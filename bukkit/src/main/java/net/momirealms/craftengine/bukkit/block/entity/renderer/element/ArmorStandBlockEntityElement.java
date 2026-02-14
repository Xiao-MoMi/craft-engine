package net.momirealms.craftengine.bukkit.block.entity.renderer.element;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
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
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ArmorStandBlockEntityElement implements BlockEntityElement {
    public final ArmorStandBlockEntityElementConfig config;
    public final Object cachedSpawnPacket;
    public final Object cachedDespawnPacket;
    public final Object cachedUpdatePosPacket;
    public final Object cachedScalePacket;
    public final Object cachedTeamPacket;
    public final int entityId;
    public final UUID uuid = UUID.randomUUID();

    public ArmorStandBlockEntityElement(ArmorStandBlockEntityElementConfig config, BlockPos pos) {
        this(config, pos, EntityProxy.ENTITY_COUNTER.incrementAndGet(), false);
    }

    public ArmorStandBlockEntityElement(ArmorStandBlockEntityElementConfig config, BlockPos pos, int entityId, boolean posChanged) {
        Vector3f position = config.position();
        this.cachedSpawnPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityId, this.uuid, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z,
                config.xRot(), config.yRot(), EntityTypeProxy.ARMOR_STAND, 0, Vec3Proxy.ZERO, config.yRot()
        );
        this.config = config;
        this.cachedDespawnPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(entityId));
        this.entityId = entityId;
        this.cachedUpdatePosPacket = posChanged ? EntityUtils.createUpdatePosPacket(this.entityId, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z, config.yRot(), config.xRot(), false) : null;
        if (VersionHelper.isOrAbove1_20_5() && config.scale() != 1) {
            Object attributeIns = AttributeInstanceProxy.INSTANCE.newInstance$0(AttributesProxy.SCALE, $ -> {});
            AttributeInstanceProxy.INSTANCE.setBaseValue(attributeIns, config.scale());
            this.cachedScalePacket = ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance(entityId, Collections.singletonList(attributeIns));
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
    public void hide(Player player) {
        player.sendPacket(this.cachedDespawnPacket, false);
    }

    @Override
    public void show(Player player) {
        player.sendPackets(List.of(this.cachedSpawnPacket, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player))), false);
        player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player).getLiteralObject())
        )), false);
        if (this.cachedDespawnPacket != null) {
            player.sendPacket(this.cachedDespawnPacket, false);
        }
        if (this.cachedTeamPacket != null) {
            player.sendPacket(this.cachedTeamPacket, false);
        }
    }

    @Override
    public void transform(Player player) {
        if (this.cachedUpdatePosPacket != null) {
            player.sendPackets(List.of(
                    this.cachedUpdatePosPacket,
                    ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player)),
                    ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                            Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player).getLiteralObject())
                    ))
            ), false);
        } else {
            player.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, this.config.metadataValues(player)), false);
            player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(this.entityId, List.of(
                    Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player).getLiteralObject())
            )), false);
        }
    }
}
