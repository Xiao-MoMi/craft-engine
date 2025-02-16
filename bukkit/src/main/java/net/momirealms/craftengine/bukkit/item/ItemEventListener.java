package net.momirealms.craftengine.bukkit.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ItemEventListener implements Listener {
    private final BukkitCraftEngine plugin;

    public ItemEventListener(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteractBlock(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = Objects.requireNonNull(event.getClickedBlock());
        Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
        int stateId = BlockStateUtils.blockStateToId(blockState);
        if (BlockStateUtils.isVanillaBlock(stateId)) {
            return;
        }

        // it's breaking the block
        if (action == Action.LEFT_CLICK_BLOCK && event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        CustomBlockInteractEvent interactEvent = new CustomBlockInteractEvent(
                event.getPlayer(),
                block.getLocation(),
                event.getInteractionPoint(),
                BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId),
                block,
                event.getBlockFace(),
                event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                action == Action.RIGHT_CLICK_BLOCK ? CustomBlockInteractEvent.Action.RIGHT_CLICK : CustomBlockInteractEvent.Action.LEFT_CLICK
        );
        if (EventUtils.fireAndCheckCancel(interactEvent)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == Event.Result.DENY || event.useInteractedBlock() == Event.Result.DENY) return;
        Location interactionPoint = event.getInteractionPoint();
        if (interactionPoint == null) return;
        Player bukkitPlayer = event.getPlayer();
        Block clickedBlock = Objects.requireNonNull(event.getClickedBlock());
        BukkitServerPlayer player = this.plugin.adapt(bukkitPlayer);
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (hand == InteractionHand.OFF_HAND) {
            int currentTicks = player.gameTicks();
            // The client will send multiple packets to the server if the client thinks it should
            // However, if the main hand item interaction is successful, the off-hand item should be blocked.
            if (!player.updateLastSuccessfulInteractionTick(currentTicks)) {
                event.setCancelled(true);
                return;
            }
        }

        // Gets the item in hand
        Item<ItemStack> itemInHand = player.getItemInHand(hand);
        if (itemInHand == null) return;
        Optional<List<ItemBehavior>> optionalItemBehaviors = itemInHand.getItemBehavior();

        // has custom item behavior
        if (optionalItemBehaviors.isPresent()) {
            BlockPos pos = LocationUtils.toBlockPos(clickedBlock.getLocation());
            Vec3d vec3d = new Vec3d(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            Direction direction = DirectionUtils.toDirection(event.getBlockFace());
            BlockHitResult hitResult = new BlockHitResult(vec3d, direction, pos, false);
            boolean interactable = InteractUtils.isInteractable(BlockStateUtils.getBlockOwnerId(clickedBlock), bukkitPlayer, clickedBlock.getBlockData(), hitResult, itemInHand);

            // do not allow to place block if it's a vanilla block
            if (itemInHand.isBlockItem() && itemInHand.isCustomItem()) {
                if (!interactable || player.isSecondaryUseActive()) {
                    event.setCancelled(true);
                }
            }

            if (!player.isSecondaryUseActive() && interactable) {
                // if it's interactable on server, cancel the custom behaviors
                return;
            }

            // TODO We need to further investigate how to handle adventure mode
            // no spectator interactions
            if (player.isSpectatorMode() || player.isAdventureMode()) {
                return;
            }

            for (ItemBehavior itemBehavior : optionalItemBehaviors.get()) {
                InteractionResult result = itemBehavior.useOnBlock(new UseOnContext(player, hand, hitResult));
                if (result == InteractionResult.SUCCESS_AND_CANCEL) {
                    event.setCancelled(true);
                    return;
                }
                int maxY = player.level().worldHeight().getMaxBuildHeight() - 1;
                if (direction == Direction.UP
                        && result != InteractionResult.SUCCESS
                        && pos.y() >= maxY
                        && itemBehavior instanceof BlockItemBehavior
                ) {
                    player.sendActionBar(Component.translatable("build.tooHigh").arguments(Component.text(maxY)).color(NamedTextColor.RED));
                    return;
                }
                if (result != InteractionResult.PASS) {
                    return;
                }
            }
            return;
        }

        // it's a vanilla block
        if (itemInHand.isBlockItem() && !itemInHand.isCustomItem()) {
            // client won't have sounds if the fake block is interactable
            // so we should check and resend sounds on interact
            Object blockState = BlockStateUtils.blockDataToBlockState(clickedBlock.getBlockData());
            int stateId = BlockStateUtils.blockStateToId(blockState);
            ImmutableBlockState againCustomBlock = BukkitBlockManager.instance().getImmutableBlockState(stateId);
            if (againCustomBlock == null || againCustomBlock.isEmpty()) {
                return;
            }
            BlockPos pos = LocationUtils.toBlockPos(clickedBlock.getLocation());
            Vec3d vec3d = new Vec3d(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            Direction direction = DirectionUtils.toDirection(event.getBlockFace());
            BlockHitResult hitResult = new BlockHitResult(vec3d, direction, pos, false);
            try {
                BlockData craftBlockData = BlockStateUtils.createBlockData(againCustomBlock.vanillaBlockState().handle());
                if (InteractUtils.isInteractable(Key.of(clickedBlock.getType().getKey().asString()), bukkitPlayer, craftBlockData, hitResult, itemInHand)) {
                    if (!player.isSecondaryUseActive()) {
                        player.setResendSound();
                    }
                } else {
                    if (BlockStateUtils.isReplaceable(againCustomBlock.customBlockState().handle()) && !BlockStateUtils.isReplaceable(againCustomBlock.vanillaBlockState().handle())) {
                        player.setResendSwing();
                    }
                }
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to get CraftBlockData", e);
            }
        }
    }
}
