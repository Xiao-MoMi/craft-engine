package net.momirealms.craftengine.bukkit.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.Location;
import org.bukkit.Material;
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

import javax.annotation.Nullable;
import java.util.*;

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

        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        CustomBlockInteractEvent interactEvent = new CustomBlockInteractEvent(
                event.getPlayer(),
                block.getLocation(),
                event.getInteractionPoint(),
                BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId),
                hand,
                action == Action.RIGHT_CLICK_BLOCK ? CustomBlockInteractEvent.Action.RIGHT_CLICK : CustomBlockInteractEvent.Action.LEFT_CLICK
        );
        if (EventUtils.fireAndCheckCancel(interactEvent)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        Location interactionPoint = event.getInteractionPoint();
        if (interactionPoint == null) return;
        Player bukkitPlayer = event.getPlayer();
        Block clickedBlock = Objects.requireNonNull(event.getClickedBlock());
        BukkitServerPlayer player = this.plugin.adapt(bukkitPlayer);
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (hand == InteractionHand.OFF_HAND) {
            int currentTicks = player.gameTicks();
            if (!player.updateLastSuccessfulInteractionTick(currentTicks)) {
                event.setCancelled(true);
                return;
            }
        }

        Item<ItemStack> itemInHand = player.getItemInHand(hand);
        if (itemInHand == null) return;
        Optional<CustomItem<ItemStack>> customItem = itemInHand.getCustomItem();

        Material material = itemInHand.getItem().getType();
        // is custom item
        if (customItem.isPresent()) {
            if (material.isBlock()) {
                event.setCancelled(true);
            }
            CustomItem<ItemStack> item = customItem.get();
            BlockPos pos = LocationUtils.toBlockPos(clickedBlock.getLocation());
            Vec3d vec3d = new Vec3d(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            Direction direction = DirectionUtils.toDirection(event.getBlockFace());
            BlockHitResult hitResult = new BlockHitResult(vec3d, direction, pos, false);
            if (!player.isSecondaryUseActive()) {
                if (InteractUtils.isInteractable(BlockStateUtils.getRealBlockId(clickedBlock), bukkitPlayer, clickedBlock.getBlockData(), hitResult, itemInHand)) return;
            }
            InteractionResult result = item.behavior().useOnBlock(new UseOnContext(player, hand, hitResult));
            int maxY = player.level().worldHeight().getMaxBuildHeight() - 1;
            if (direction == Direction.UP
                    && result != InteractionResult.SUCCESS
                    && pos.y() >= maxY
                    && item.behavior() instanceof BlockItemBehavior
            ) {
                player.sendActionBar(Component.translatable("build.tooHigh").arguments(Component.text(maxY)).color(NamedTextColor.RED));
            }
        } else if (material.isBlock()) {
            // vanilla item
            // client won't have sounds if the fake block is interactable
            Object blockState = BlockStateUtils.blockDataToBlockState(clickedBlock.getBlockData());
            int stateId = BlockStateUtils.blockStateToId(blockState);
            if (BlockStateUtils.isVanillaBlock(stateId)) {
                return;
            }
            ImmutableBlockState againCustomBlock = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId);
            if (againCustomBlock.isEmpty()) {
                return;
            }
            BlockPos pos = LocationUtils.toBlockPos(clickedBlock.getLocation());
            Vec3d vec3d = new Vec3d(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            Direction direction = DirectionUtils.toDirection(event.getBlockFace());
            BlockHitResult hitResult = new BlockHitResult(vec3d, direction, pos, false);
            try {
                BlockData craftBlockData = (BlockData) Reflections.method$CraftBlockData$createData.invoke(null, againCustomBlock.vanillaBlockState().handle());
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

    @EventHandler(ignoreCancelled = true)
    public void onUseDebugStick(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        ItemStack itemInHand = event.getItem();
        if (itemInHand == null) return;
        Material material = itemInHand.getType();
        if (material != Material.DEBUG_STICK) return;
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = this.plugin.adapt(bukkitPlayer);
        if (!(player.canInstabuild() && player.hasPermission("minecraft.debugstick")) && !player.hasPermission("minecraft.debugstick.always")) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            int currentTicks = player.gameTicks();
            if (!player.updateLastSuccessfulInteractionTick(currentTicks)) {
                event.setCancelled(true);
                return;
            }
        }
        Object blockState = BlockStateUtils.blockDataToBlockState(clickedBlock.getBlockData());
        int stateId = BlockStateUtils.blockStateToId(blockState);
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            event.setCancelled(true);
            boolean update = event.getAction() == Action.RIGHT_CLICK_BLOCK;
            ImmutableBlockState clickedCustomBlock = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId);
            CustomBlock block = clickedCustomBlock.owner().value();
            Collection<Property<?>> properties = block.properties();
            String blockId = block.id().toString();
            try {
                if (properties.isEmpty()) {
                    Object systemChatPacket = Reflections.constructor$ClientboundSystemChatPacket.newInstance(
                            ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.empty").arguments(Component.text(blockId))), true);
                    player.sendPacket(systemChatPacket, false);
                } else {
                    Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(itemInHand);
                    Object storedData = wrapped.getTag("craftengine:debug_stick_state");
                    if (storedData == null) storedData = new HashMap<>();
                    if (storedData instanceof Map<?,?> map) {
                        Map<String, Object> data = MiscUtils.castToMap(map, false);
                        String currentPropertyName = (String) data.get(blockId);
                        Property<?> currentProperty = block.getProperty(currentPropertyName);
                        if (currentProperty == null) {
                            currentProperty = properties.iterator().next();
                        }
                        if (update) {
                            ImmutableBlockState nextState = cycleState(clickedCustomBlock, currentProperty, player.isSecondaryUseActive());
                            CraftEngineBlocks.place(clickedBlock.getLocation(), nextState, new UpdateOption.Builder().updateClients().updateKnownShape().build());
                            Object systemChatPacket = Reflections.constructor$ClientboundSystemChatPacket.newInstance(
                                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.update")
                                            .arguments(
                                                    Component.text(currentProperty.name()),
                                                    Component.text(getNameHelper(nextState, currentProperty))
                                            )), true);
                            player.sendPacket(systemChatPacket, false);
                        } else {
                            currentProperty = getRelative(properties, currentProperty, player.isSecondaryUseActive());
                            data.put(blockId, currentProperty.name());
                            wrapped.setTag(data, "craftengine:debug_stick_state");
                            wrapped.load();
                            Object systemChatPacket = Reflections.constructor$ClientboundSystemChatPacket.newInstance(
                                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.select")
                                            .arguments(
                                                    Component.text(currentProperty.name()),
                                                    Component.text(getNameHelper(clickedCustomBlock, currentProperty))
                                            )), true);
                            player.sendPacket(systemChatPacket, false);
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to send system chat packet", e);
            }
        }
    }

    private static <T extends Comparable<T>> ImmutableBlockState cycleState(ImmutableBlockState state, Property<T> property, boolean inverse) {
        return state.with(property, getRelative(property.possibleValues(), state.get(property), inverse));
    }

    private static <T> T getRelative(Iterable<T> elements, @Nullable T current, boolean inverse) {
        return inverse ? MCUtils.findPreviousInIterable(elements, current) : MCUtils.findNextInIterable(elements, current);
    }

    private static <T extends Comparable<T>> String getNameHelper(ImmutableBlockState state, Property<T> property) {
        return property.valueName(state.get(property));
    }
}
