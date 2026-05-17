package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BukkitItemUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.behavior.FurnitureItem;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBlockUpdatePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ServerboundUseItemOnPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerGamePacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Optional;

public final class InteractListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove26_1 ? new V26_1() : new V1_20();

    private InteractListener() {}

    private static class V1_20 implements ByteBufferPacketListener {

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
            int actionType = buf.readVarInt();
            // 太远就是挂
            if (!furniture.canInteract(serverPlayer)) return;

            if (actionType == 1) {
                // ATTACK
                boolean usingSecondaryAction = buf.readBoolean();
                if (entityId != furniture.entityId()) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(furniture.entityId());
                    buf.writeVarInt(actionType);
                    buf.writeBoolean(usingSecondaryAction);
                }

                AttackListener.performAttackFurniture(furniture, entityId, serverPlayer);
            } else if (actionType == 2) {
                // INTERACT_AT
                float x = buf.readFloat();
                float y = buf.readFloat();
                float z = buf.readFloat();
                InteractionHand hand = buf.readVarInt() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                boolean usingSecondaryAction = buf.readBoolean();
                if (entityId != furniture.entityId()) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(furniture.entityId());
                    buf.writeVarInt(actionType);
                    buf.writeFloat(x).writeFloat(y).writeFloat(z);
                    buf.writeVarInt(hand == InteractionHand.MAIN_HAND ? 0 : 1);
                    buf.writeBoolean(usingSecondaryAction);
                }

                performInteractFurniture(furniture, entityId, serverPlayer, hand, usingSecondaryAction);
            } else if (actionType == 0) {
                // INTERACT
                int hand = buf.readVarInt();
                boolean usingSecondaryAction = buf.readBoolean();
                if (entityId != furniture.entityId()) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(furniture.entityId());
                    buf.writeVarInt(actionType);
                    buf.writeVarInt(hand);
                    buf.writeBoolean(usingSecondaryAction);
                }
            }
        }
    }

    private static class V26_1 implements ByteBufferPacketListener {
        private V26_1() {}

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
            // 太远就是挂
            if (!furniture.canInteract(serverPlayer)) return;

            InteractionHand hand = buf.readVarInt() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            Vec3d vec3d = buf.readLpVec3();
            boolean usingSecondaryAction = buf.readBoolean();

            if (entityId != furniture.entityId()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(furniture.entityId());
                buf.writeVarInt(hand == InteractionHand.MAIN_HAND ? 0 : 1);
                buf.writeLpVec3(vec3d);
                buf.writeBoolean(usingSecondaryAction);
            }

            performInteractFurniture(furniture, entityId, serverPlayer, hand, usingSecondaryAction);
        }
    }

    private static void performInteractFurniture(BukkitFurniture furniture,
                                                int entityId,
                                                BukkitServerPlayer serverPlayer,
                                                InteractionHand hand,
                                                boolean usingSecondaryAction) {
        Location location = furniture.location();

        Runnable mainThreadTask = () -> {
            if (!furniture.isValid()) {
                return;
            }

            // 先检查碰撞箱部分是否存在
            FurnitureHitBox hitBox = furniture.hitboxByEntityId(entityId);
            if (hitBox == null) return;
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

            Player platformPlayer = serverPlayer.platformPlayer();
            if (platformPlayer == null) return;
            // 检测能否交互碰撞箱
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
            // 触发事件
            ContextHolder.Builder contextBuilder = ContextHolder.builder();
            FurnitureInteractEvent interactEvent = new FurnitureInteractEvent(serverPlayer.platformPlayer(), furniture, hand, interactionPoint, hitBox, contextBuilder);
            if (EventUtils.fireAndCheckCancel(interactEvent)) {
                return;
            }

            if (!furniture.isValid()) {
                return;
            }

            // 执行家具行为
            InteractEntityContext interactEntityContext = new InteractEntityContext(serverPlayer, hand, hitResult);
            InteractionResult result = furniture.controller.useOnFurniture(hitBox, interactEntityContext);
            if (result.success()) {
                serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                return;
            }
            if (result == InteractionResult.TRY_EMPTY_HAND && hand == InteractionHand.MAIN_HAND) {
                result = furniture.controller.useWithoutItem(interactEntityContext);
                if (result.success()) {
                    serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                    return;
                }
            }
            if (result == InteractionResult.FAIL) {
                return;
            }

            // 执行事件动作
            Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
            Cancellable cancellable = Cancellable.of(interactEvent::isCancelled, interactEvent::setCancelled);
            // execute functions
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer,
                    contextBuilder
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withParameter(DirectContextParameters.FURNITURE, furniture)
                            .withParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                            .withParameter(DirectContextParameters.HAND, hand)
                            .withParameter(DirectContextParameters.POSITION, furniture.position())
            );
            furniture.config().execute(context, EventTrigger.RIGHT_CLICK);
            if (cancellable.isCancelled()) {
                return;
            }
            // 不处理调试棒
            if (BukkitItemUtils.isDebugStick(itemInHand)) {
                return;
            }
            // 已经有过交互了
            if (serverPlayer.lastSuccessfulInteractionTick() == serverPlayer.gameTicks()) {
                return;
            }
            // 必须从网络包层面处理，否则无法获取交互的具体实体
            if (usingSecondaryAction && !itemInHand.isEmpty() && hitBox.config().canUseItemOn()) {
                Optional<ItemDefinition> optionalItemDefinition = itemInHand.getDefinition();
                if (optionalItemDefinition.isPresent()) {
                    ItemDefinition itemDefinition = optionalItemDefinition.get();
                    FurnitureItem firstFurniture = itemDefinition.behavior().getFirst(FurnitureItem.class);
                    if (firstFurniture != null) {
                        ((ItemBehavior) firstFurniture).useOnBlock(new UseOnContext(serverPlayer, InteractionHand.MAIN_HAND, new BlockHitResult(hitResult.hitLocation(), hitResult.direction(), BlockPos.fromVec3d(hitResult.hitLocation()), false)));
                        return;
                    }
                }

                // 模拟原版物品交互行为
                serverPlayer.setResendSound();

                {
                    Object nmsPlayer = serverPlayer.serverPlayer();
                    Object serverLevel = ServerPlayerProxy.INSTANCE.getLevel(nmsPlayer);
                    Object blockPos = LocationUtils.toBlockPos(hitResult.blockPos());
                    Object previousBlockState = ServerLevelProxy.INSTANCE.getBlockStateIfLoaded(serverLevel, blockPos);
                    if (previousBlockState != null) {
                        Object clickedPoint = Vec3Proxy.INSTANCE.newInstance(hitResult.hitLocation().x, hitResult.hitLocation().y, hitResult.hitLocation().z);
                        Object nmsDirection = DirectionUtils.toNMSDirection(hitResult.direction());
                        Object useItemPacket = ServerboundUseItemOnPacketProxy.INSTANCE.newInstance(
                                InteractionHandProxy.MAIN_HAND,
                                BlockHitResultProxy.INSTANCE.newInstance(clickedPoint, nmsDirection, blockPos, false),
                                0
                        );
                        try {
                            ServerLevelProxy.INSTANCE.setBlock(serverLevel, blockPos, BlockProxy.INSTANCE.getDefaultBlockState(BlocksProxy.COBWEB), UpdateFlags.UPDATE_INVISIBLE);
                            ServerboundUseItemOnPacketProxy.INSTANCE.setTimestamp(useItemPacket, System.currentTimeMillis());
                            ServerGamePacketListenerImplProxy.INSTANCE.handleUseItemOn(ServerPlayerProxy.INSTANCE.getConnection(nmsPlayer), useItemPacket);
                        } finally {
                            ServerLevelProxy.INSTANCE.setBlock(serverLevel, blockPos, previousBlockState, UpdateFlags.UPDATE_INVISIBLE);
                            serverPlayer.sendPacket(ClientboundBlockUpdatePacketProxy.INSTANCE.newInstance$1(serverLevel, blockPos), false);
                        }
                    }
                }

                if (!part.interactive()) {
                    serverPlayer.swingHand(InteractionHand.MAIN_HAND);
                }
            } else {
                if (!usingSecondaryAction) {
                    for (Seat<FurnitureHitBox> seat : hitBox.seats()) {
                        if (!seat.isOccupied()) {
                            if (seat.spawnSeat(serverPlayer, furniture.position())) {
                                if (!part.interactive()) {
                                    serverPlayer.swingHand(InteractionHand.MAIN_HAND);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        };
        BukkitCraftEngine.instance().scheduler().platform().run(mainThreadTask, location);
    }
}
