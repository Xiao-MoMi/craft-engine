package net.momirealms.craftengine.bukkit.block.entity.renderer.element;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
        this.cachedSpawnPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId, this.uuid, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z,
                config.xRot(), config.yRot(), MEntityTypes.ARMOR_STAND, 0, Vec3Proxy.ZERO, config.yRot()
        );
        this.config = config;
        this.cachedDespawnPacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(IntList.of(entityId));
        this.entityId = entityId;
        this.cachedUpdatePosPacket = posChanged ? FastNMS.INSTANCE.constructor$ClientboundEntityPositionSyncPacket(this.entityId, pos.x() + position.x, pos.y() + position.y, pos.z() + position.z, config.yRot(), config.xRot(), false) : null;
        if (VersionHelper.isOrAbove1_20_5() && config.scale() != 1) {
            Object attributeIns = FastNMS.INSTANCE.constructor$AttributeInstance(MAttributeHolders.SCALE, (Consumer<?>) (o) -> {});
            FastNMS.INSTANCE.method$AttributeInstance$setBaseValue(attributeIns, config.scale());
            this.cachedScalePacket = FastNMS.INSTANCE.constructor$ClientboundUpdateAttributesPacket(entityId, Collections.singletonList(attributeIns));
        } else {
            this.cachedScalePacket = null;
        }
        Object teamPacket = null;
        if (config.glowColor != null) {
            Object teamByColor = BukkitTeamManager.instance().getTeamByColor(config.glowColor);
            if (teamByColor != null) {
                teamPacket = FastNMS.INSTANCE.method$ClientboundSetPlayerTeamPacket$createMultiplePlayerPacket(teamByColor, List.of(this.uuid.toString()), true);
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
        player.sendPackets(List.of(this.cachedSpawnPacket, FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, this.config.metadataValues(player))), false);
        player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(this.entityId, List.of(
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
                    FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, this.config.metadataValues(player)),
                    FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(this.entityId, List.of(
                            Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player).getLiteralObject())
                    ))
            ), false);
        } else {
            player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, this.config.metadataValues(player)), false);
            player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(this.entityId, List.of(
                    Pair.of(EquipmentSlotProxy.HEAD, this.config.item(player).getLiteralObject())
            )), false);
        }
    }
}
