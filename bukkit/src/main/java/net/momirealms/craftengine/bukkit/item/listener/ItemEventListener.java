package net.momirealms.craftengine.bukkit.item.listener;

import io.papermc.paper.event.block.CompostItemEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent;
import net.momirealms.craftengine.bukkit.item.BukkitCustomItem;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitBlockInWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.item.setting.FoodData;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ItemEventListener implements Listener {
    private final BukkitCraftEngine plugin;

    public ItemEventListener(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        BukkitServerPlayer serverPlayer = this.plugin.adapt(event.getPlayer());
        if (serverPlayer == null) return;
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        Item<ItemStack> itemInHand = serverPlayer.getItemInHand(hand);

        if (itemInHand == null) return;
        Optional<CustomItem<ItemStack>> optionalCustomItem = itemInHand.getCustomItem();
        if (optionalCustomItem.isEmpty()) return;

        Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
        PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                .withParameter(DirectContextParameters.EVENT, cancellable)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(event.getRightClicked().getLocation()))
                .withParameter(DirectContextParameters.HAND, hand)
        );
        CustomItem<ItemStack> customItem = optionalCustomItem.get();
        customItem.execute(context, EventTrigger.RIGHT_CLICK);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteractBlock(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (
                (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) ||  /* block is required */
                (player.getGameMode() == GameMode.SPECTATOR) ||  /* no spectator interactions */
                (action == Action.LEFT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE) /* it's breaking the block */
        ) {
            return;
        }

        BukkitServerPlayer serverPlayer = this.plugin.adapt(player);
        if (serverPlayer == null) return;
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        // 如果本tick内主手已被处理，则不处理副手
        // 这是因为客户端可能会同时发主副手交互包，但实际上只能处理其中一个
        if (this.cancelEventIfHasInteraction(event, serverPlayer, hand)) {
            return;
        }

        // some common data
        Block block = Objects.requireNonNull(event.getClickedBlock());
        BlockData blockData = block.getBlockData();
        Object blockState = BlockStateUtils.blockDataToBlockState(blockData);
        ImmutableBlockState immutableBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        Item<ItemStack> itemInHand = serverPlayer.getItemInHand(hand);
        Location interactionPoint = event.getInteractionPoint();

        BlockHitResult hitResult = null;
        if (action == Action.RIGHT_CLICK_BLOCK && interactionPoint != null) {
            Direction direction = DirectionUtils.toDirection(event.getBlockFace());
            BlockPos pos = LocationUtils.toBlockPos(block.getLocation());
            Vec3d vec3d = new Vec3d(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            hitResult = new BlockHitResult(vec3d, direction, pos, false);
        }

        // 处理自定义方块
        if (immutableBlockState != null) {
            // call the event if it's custom
            CustomBlockInteractEvent interactEvent = new CustomBlockInteractEvent(
                    player,
                    block.getLocation(),
                    interactionPoint,
                    immutableBlockState,
                    block,
                    event.getBlockFace(),
                    hand,
                    action.isRightClick() ? CustomBlockInteractEvent.Action.RIGHT_CLICK : CustomBlockInteractEvent.Action.LEFT_CLICK,
                    event.getItem()
            );
            if (EventUtils.fireAndCheckCancel(interactEvent)) {
                event.setCancelled(true);
                return;
            }

            // fix client side issues
            if (action.isRightClick() && hitResult != null &&
                    InteractUtils.willConsume(player, BlockStateUtils.fromBlockData(immutableBlockState.vanillaBlockState().handle()), hitResult, itemInHand)) {
                player.updateInventory();
                //PlayerUtils.resendItemInHand(player);
            }

            Cancellable dummy = Cancellable.dummy();
            // run custom functions
            CustomBlock customBlock = immutableBlockState.owner().value();
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                    .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                    .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, immutableBlockState)
                    .withParameter(DirectContextParameters.HAND, hand)
                    .withParameter(DirectContextParameters.EVENT, dummy)
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                    .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
            );
            if (action.isRightClick()) customBlock.execute(context, EventTrigger.RIGHT_CLICK);
            else customBlock.execute(context, EventTrigger.LEFT_CLICK);
            if (dummy.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            if (hitResult != null) {
                UseOnContext useOnContext = new UseOnContext(serverPlayer, hand, itemInHand, hitResult);
                boolean hasItem = player.getInventory().getItemInMainHand().getType() != Material.AIR || player.getInventory().getItemInOffHand().getType() != Material.AIR;
                boolean flag = player.isSneaking() && hasItem;
                if (!flag) {
                    if (immutableBlockState.behavior() instanceof AbstractBlockBehavior behavior) {
                        InteractionResult result = behavior.useOnBlock(useOnContext, immutableBlockState);
                        if (result.success()) {
                            serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                            if (result == InteractionResult.SUCCESS_AND_CANCEL) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        if (result == InteractionResult.TRY_EMPTY_HAND && hand == InteractionHand.MAIN_HAND) {
                            result = behavior.useWithoutItem(useOnContext, immutableBlockState);
                            if (result.success()) {
                                serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                                if (result == InteractionResult.SUCCESS_AND_CANCEL) {
                                    event.setCancelled(true);
                                }
                                return;
                            }
                        }
                        if (result == InteractionResult.FAIL) {
                            return;
                        }
                    }
                }
            }
        } else {
            if (Config.enableSoundSystem() && hitResult != null) {
                Object blockOwner = FastNMS.INSTANCE.method$BlockState$getBlock(blockState);
                if (this.plugin.blockManager().isOpenableBlockSoundRemoved(blockOwner)) {
                    boolean hasItem = player.getInventory().getItemInMainHand().getType() != Material.AIR || player.getInventory().getItemInOffHand().getType() != Material.AIR;
                    boolean flag = player.isSneaking() && hasItem;
                    if (!flag) {
                        if (blockData instanceof Openable openable) {
                            SoundData soundData = this.plugin.blockManager().getRemovedOpenableBlockSound(blockOwner, !openable.isOpen());
                            serverPlayer.playSound(soundData.id(), SoundSource.BLOCK, soundData.volume().get(), soundData.pitch().get());
                        }
                    }
                }
            }
        }

        Optional<CustomItem<ItemStack>> optionalCustomItem = itemInHand == null ? Optional.empty() : itemInHand.getCustomItem();
        boolean hasItem = itemInHand != null;
        boolean hasCustomItem = optionalCustomItem.isPresent();

        // interact block with items
        if (hasItem && action == Action.RIGHT_CLICK_BLOCK) {
            // some plugins would trigger this event without interaction point
            if (interactionPoint == null) {
                if (hasCustomItem) {
                    event.setCancelled(true);
                }
                return;
            }

            // handle block item
            if (itemInHand.isBlockItem()) {
                // vanilla item
                if (!hasCustomItem) {
                    // interact a custom block
                    if (immutableBlockState != null) {
                        // client won't have sounds if the clientside block is interactable
                        // so we should check and resend sounds on BlockPlaceEvent
                        BlockData craftBlockData = BlockStateUtils.fromBlockData(immutableBlockState.vanillaBlockState().handle());
                        if (InteractUtils.isInteractable(player, craftBlockData, hitResult, itemInHand)) {
                            if (!serverPlayer.isSecondaryUseActive()) {
                                serverPlayer.setResendSound();
                            }
                        } else {
                            if (BlockStateUtils.isReplaceable(immutableBlockState.customBlockState().handle()) && !BlockStateUtils.isReplaceable(immutableBlockState.vanillaBlockState().handle())) {
                                serverPlayer.setResendSwing();
                            }
                        }
                    }
                }
                // custom item
                else {
                    if (optionalCustomItem.get().settings().canPlaceRelatedVanillaBlock()) {
                        // 如果用户设置了允许放置对应的原版方块，那么直接返回。
                        // 这种情况下最好是return，以避免同时触发多个behavior发生冲突
                        // 当用户选择其作为原版方块放下时，自定义行为可能已经不重要了？
                        return;
                    } else {
                        // todo 实际上这里的处理并不正确，因为判断玩家是否能够放置那个方块需要更加细节的判断。比如玩家无法对着树叶放置火把，但是交互事件依然触发，此情况下不可丢弃自定义行为。
                        if (serverPlayer.isSecondaryUseActive() || !InteractUtils.isInteractable(player, blockData, hitResult, itemInHand)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }

            // execute item right click functions
            if (hasCustomItem) {
                Cancellable dummy = Cancellable.dummy();
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                        .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                        .withOptionalParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, immutableBlockState)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                        .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                        .withParameter(DirectContextParameters.HAND, hand)
                        .withParameter(DirectContextParameters.EVENT, dummy)
                );
                CustomItem<ItemStack> customItem = optionalCustomItem.get();
                customItem.execute(context, EventTrigger.RIGHT_CLICK);
                if (dummy.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
            }

            // 检查其他的物品行为，物品行为理论只在交互时处理
            Optional<List<ItemBehavior>> optionalItemBehaviors = itemInHand.getItemBehavior();
            // 物品类型是否包含自定义物品行为，行为不一定来自于自定义物品，部分原版物品也包含了新的行为
            if (optionalItemBehaviors.isPresent()) {
                // 检测是否可交互应当只判断原版方块，因为自定义方块早就判断过了，如果可交互不可能到这一步
                boolean interactable = immutableBlockState == null && InteractUtils.isInteractable(player, blockData, hitResult, itemInHand);
                // 如果方块可交互但是玩家没shift，那么原版的方块交互优先，取消自定义物品的behavior
                // todo 如果我的物品行为允许某些交互呢？是否值得进一步处理？
                if (!serverPlayer.isSecondaryUseActive() && interactable) {
                    return;
                }
                UseOnContext useOnContext = new UseOnContext(serverPlayer, hand, itemInHand, hitResult);
                // 依次执行物品行为
                for (ItemBehavior itemBehavior : optionalItemBehaviors.get()) {
                    InteractionResult result = itemBehavior.useOnBlock(useOnContext);
                    if (result.success()) {
                        serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                    }
                    if (result == InteractionResult.SUCCESS_AND_CANCEL) {
                        event.setCancelled(true);
                        return;
                    }
                    if (result != InteractionResult.PASS) {
                        return;
                    }
                }
            }
        }

        if (hasCustomItem && action == Action.LEFT_CLICK_BLOCK) {
            Cancellable dummy = Cancellable.dummy();
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                    .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                    .withOptionalParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, immutableBlockState)
                    .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                    .withParameter(DirectContextParameters.HAND, hand)
            );
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            customItem.execute(context, EventTrigger.LEFT_CLICK);
            if (dummy.isCancelled()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAir(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.LEFT_CLICK_AIR)
            return;
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = this.plugin.adapt(player);
        if (serverPlayer.isSpectatorMode())
            return;
        // Gets the item in hand
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        Item<ItemStack> itemInHand = serverPlayer.getItemInHand(hand);
        // should never be null
        if (itemInHand == null) return;

        // todo 真的需要这个吗
        if (cancelEventIfHasInteraction(event, serverPlayer, hand)) {
            return;
        }

        Optional<CustomItem<ItemStack>> optionalCustomItem = itemInHand.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                    .withParameter(DirectContextParameters.HAND, hand)
                    .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(player.getLocation()))
            );
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            if (action.isRightClick()) customItem.execute(context, EventTrigger.RIGHT_CLICK);
            else customItem.execute(context, EventTrigger.LEFT_CLICK);
        }

        if (action.isRightClick()) {
            Optional<List<ItemBehavior>> optionalItemBehaviors = itemInHand.getItemBehavior();
            if (optionalItemBehaviors.isPresent()) {
                for (ItemBehavior itemBehavior : optionalItemBehaviors.get()) {
                    InteractionResult result = itemBehavior.use(serverPlayer.world(), serverPlayer, hand);
                    if (result.success()) {
                        serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                    }
                    if (result == InteractionResult.SUCCESS_AND_CANCEL) {
                        event.setCancelled(true);
                        return;
                    }
                    if (result != InteractionResult.PASS) {
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        ItemStack consumedItem = event.getItem();
        if (ItemUtils.isEmpty(consumedItem)) return;
        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(consumedItem);
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) {
            return;
        }
        Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
        CustomItem<ItemStack> customItem = optionalCustomItem.get();
        PlayerOptionalContext context = PlayerOptionalContext.of(this.plugin.adapt(event.getPlayer()), ContextHolder.builder()
                .withParameter(DirectContextParameters.ITEM_IN_HAND, wrapped)
                .withParameter(DirectContextParameters.EVENT, cancellable)
                .withParameter(DirectContextParameters.HAND, event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)
        );
        customItem.execute(context, EventTrigger.CONSUME);
        if (event.isCancelled()) {
            return;
        }
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            Key replacement = customItem.settings().consumeReplacement();
            if (replacement == null) {
                event.setReplacement(null);
            } else {
                ItemStack replacementItem = this.plugin.itemManager().buildItemStack(replacement, this.plugin.adapt(event.getPlayer()));
                event.setReplacement(replacementItem);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (VersionHelper.isOrAbove1_20_5()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack consumedItem = event.getItem();
        if (ItemUtils.isEmpty(consumedItem)) return;
        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(consumedItem);
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) {
            return;
        }
        CustomItem<ItemStack> customItem = optionalCustomItem.get();
        FoodData foodData = customItem.settings().foodData();
        if (foodData == null) return;
        event.setCancelled(true);
        int oldFoodLevel = player.getFoodLevel();
        if (foodData.nutrition() != 0) player.setFoodLevel(MCUtils.clamp(oldFoodLevel + foodData.nutrition(), 0, 20));
        float oldSaturation = player.getSaturation();
        if (foodData.saturation() != 0) player.setSaturation(MCUtils.clamp(oldSaturation, 0, 10));
    }

    private boolean cancelEventIfHasInteraction(PlayerInteractEvent event, BukkitServerPlayer player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) {
            int currentTicks = player.gameTicks();
            // The client will send multiple packets to the server if the client thinks it should
            // However, if the main hand item interaction is successful, the off-hand item should be blocked.
            if (player.lastSuccessfulInteractionTick() == currentTicks) {
                event.setCancelled(true);
                return true;
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Item item) {
            Optional.ofNullable(this.plugin.itemManager().wrap(item.getItemStack()))
                    .flatMap(Item::getCustomItem)
                    .ifPresent(it -> {
                        if (it.settings().invulnerable().contains(DamageCauseUtils.fromBukkit(event.getCause()))) {
                            event.setCancelled(true);
                        }
                    });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEnchant(PrepareItemEnchantEvent event) {
        ItemStack itemToEnchant = event.getItem();
        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(itemToEnchant);
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) return;
        CustomItem<ItemStack> customItem = optionalCustomItem.get();
        if (!customItem.settings().canEnchant()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCompost(CompostItemEvent event) {
        ItemStack itemToCompost = event.getItem();
        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(itemToCompost);
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) return;
        event.setWillRaiseLevel(RandomUtils.generateRandomFloat(0, 1) < optionalCustomItem.get().settings().compostProbability());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof EnchantingInventory inventory)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack lazuli = inventory.getSecondary();
        if (lazuli != null) return;
        ItemStack item = inventory.getItem();
        if (item == null) return;
        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(item);
        if (wrapped == null) return;
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) return;
        BukkitCustomItem customItem = (BukkitCustomItem) optionalCustomItem.get();
        if (customItem.clientItem() == FastNMS.INSTANCE.method$ItemStack$getItem(wrapped.getLiteralObject())) return;
        BukkitServerPlayer serverPlayer = this.plugin.adapt(player);
        if (serverPlayer == null) return;
        this.plugin.scheduler().sync().runDelayed(() -> {
            Object container = FastNMS.INSTANCE.field$Player$containerMenu(serverPlayer.serverPlayer());
            if (!CoreReflections.clazz$EnchantmentMenu.isInstance(container)) return;
            Object secondSlotItem = FastNMS.INSTANCE.method$Slot$getItem(FastNMS.INSTANCE.method$AbstractContainerMenu$getSlot(container, 1));
            if (secondSlotItem == null || FastNMS.INSTANCE.method$ItemStack$isEmpty(secondSlotItem)) return;
            Object[] dataSlots = FastNMS.INSTANCE.field$AbstractContainerMenu$dataSlots(container).toArray();
            List<Object> packets = new ArrayList<>(dataSlots.length);
            for (int i = 0; i < dataSlots.length; i++) {
                Object dataSlot = dataSlots[i];
                int data = FastNMS.INSTANCE.method$DataSlot$get(dataSlot);
                packets.add(FastNMS.INSTANCE.constructor$ClientboundContainerSetDataPacket(FastNMS.INSTANCE.field$AbstractContainerMenu$containerId(container), i, data));
            }
            serverPlayer.sendPackets(packets, false);
        });
    }
}
