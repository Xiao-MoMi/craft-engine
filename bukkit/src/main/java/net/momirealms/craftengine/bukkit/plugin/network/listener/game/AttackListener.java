package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureHitEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureHitData;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Optional;

public final class AttackListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove26_1 ? new AttackListener() : null;

    private AttackListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
        if (serverPlayer.isSpectatorMode()) return;
        // 交互实体的时候，应该取消挖掘
        serverPlayer.stopMiningBlock();

        FriendlyByteBuf buf = event.getBuffer();
        int rawId = buf.readVarInt();
        int entityId = CraftEngine.instance().compatibilityManager().remapEntityId(rawId);
        BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByInteractableEntityId(entityId);
        if (furniture == null) return;

        if (entityId != furniture.entityId()) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(furniture.entityId());
        }

        if (!furniture.canInteract(serverPlayer)) return;

        performAttackFurniture(furniture, entityId, serverPlayer);
    }

    public static void performAttackFurniture(BukkitFurniture furniture, int entityId, BukkitServerPlayer serverPlayer) {
        Location location = furniture.location();

        Runnable mainThreadTask = () -> {
            if (!furniture.isValid()) {
                return;
            }

            FurnitureDefinition config = furniture.config;
            if (serverPlayer.isAdventureMode() && !config.settings().allowBreakingInAdventureMode()) {
                return;
            }

            // 先检查碰撞箱部分是否存在
            FurnitureHitBox hitBox = furniture.hitboxByEntityId(entityId);
            if (hitBox == null) return;

            Player platformPlayer = serverPlayer.platformPlayer();
            if (platformPlayer == null) return;

            if (!BukkitCraftEngine.instance().antiGriefProvider().test(platformPlayer, Flag.BREAK, location))
                return;

            FurnitureHitboxPart part = null;
            for (FurnitureHitboxPart p : hitBox.parts()) {
                if (p.entityId() == entityId) {
                    part = p;
                    break;
                }
            }
            if (part == null) {
                return;
            }

            Location eyeLocation = platformPlayer.getEyeLocation();
            Vector direction = eyeLocation.getDirection();
            Location endLocation = eyeLocation.clone();
            endLocation.add(direction.multiply(serverPlayer.getCachedInteractionRange()));
            Optional<EntityHitResult> optionalHitResult = part.aabb().clip(LocationUtils.toVec3d(eyeLocation), LocationUtils.toVec3d(endLocation));
            if (optionalHitResult.isEmpty()) {
                return;
            }
            EntityHitResult hitResult = optionalHitResult.get();
            Vec3d hitLocation = hitResult.hitLocation();
            // 获取正确的交互点
            Location interactionPoint = new Location(platformPlayer.getWorld(), hitLocation.x, hitLocation.y, hitLocation.z);

            ContextHolder.Builder contextBuilder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.FURNITURE, furniture)
                    .withParameter(DirectContextParameters.HAND, InteractionHand.MAIN_HAND)
                    .withParameter(DirectContextParameters.ITEM_IN_HAND, serverPlayer.getItemInHand(InteractionHand.MAIN_HAND))
                    .withParameter(DirectContextParameters.POSITION, furniture.position());
            FurnitureHitEvent hitEvent = new FurnitureHitEvent(serverPlayer.platformPlayer(), furniture, interactionPoint, hitBox, contextBuilder);
            if (EventUtils.fireAndCheckCancel(hitEvent))
                return;

            int hitTimes = config.settings().hitTimes();
            if (hitTimes > 1 && !serverPlayer.isCreativeMode()) {
                FurnitureHitData furnitureHitData = serverPlayer.furnitureHitData();
                int previousTimes = furnitureHitData.times(furniture.entityId());
                int alreadyHit = furnitureHitData.hit(furniture.entityId());

                // execute functions
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer,
                        contextBuilder
                                .withParameter(DirectContextParameters.EVENT, Cancellable.of(hitEvent::isCancelled, hitEvent::setCancelled))
                                .withParameter(DirectContextParameters.HIT_TIMES, alreadyHit)
                );
                config.execute(context, EventTrigger.LEFT_CLICK);
                if (hitEvent.isCancelled()) {
                    furnitureHitData.setTimes(previousTimes);
                    return;
                }

                if (alreadyHit < hitTimes) {
                    SoundData soundData = config.settings().sounds().hitSound();
                    serverPlayer.world().playSound(furniture.position(), soundData.id(), soundData.volume().get(), soundData.pitch().get(), SoundSource.PLAYER);
                    return;
                } else {
                    serverPlayer.furnitureHitData().reset();
                }
            }

            FurnitureBreakEvent breakEvent = new FurnitureBreakEvent(serverPlayer.platformPlayer(), furniture, contextBuilder);
            breakEvent.setDropItems(!serverPlayer.isCreativeMode());
            if (EventUtils.fireAndCheckCancel(breakEvent))
                return;

            // execute functions
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer,
                    contextBuilder.withParameter(DirectContextParameters.EVENT, Cancellable.of(breakEvent::isCancelled, breakEvent::setCancelled)));
            config.execute(context, EventTrigger.BREAK);
            if (breakEvent.isCancelled()) {
                return;
            }
            CraftEngineFurniture.remove(furniture, serverPlayer, breakEvent.dropItems(), true);
        };

        BukkitCraftEngine.instance().scheduler().platform().run(mainThreadTask, location);
    }
}
