package net.momirealms.craftengine.bukkit.item.listener;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackGenerateEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.entity.projectile.ProjectileItems;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.enchantment.EnchantmentKeys;
import net.momirealms.craftengine.core.item.setting.ItemSettings;
import net.momirealms.craftengine.core.item.setting.value.DragRepairItem;
import net.momirealms.craftengine.core.item.setting.value.FoodData;
import net.momirealms.craftengine.core.item.updater.ItemUpdateResult;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientboundCreativeModeTabItemsPacket;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSet;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundContainerSetDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ServerboundUseItemOnPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.DataSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.EnchantmentMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.SlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.context.UseOnContextProxy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class ItemEventListener implements Listener {
    private final BukkitCraftEngine plugin;
    private final BukkitItemManager itemManager;

    public ItemEventListener(BukkitCraftEngine plugin, BukkitItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onResourcePackGenerate(AsyncResourcePackGenerateEvent event) {
        if (Config.obfuscateItemModel()) {
            this.itemManager.persistItemModelMappings();
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.plugin.scheduler().platform().run(player::updateInventory, null, player);
                BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                if (serverPlayer != null && serverPlayer.hasClientMod()) {
                    serverPlayer.sendCustomPackets(ClientboundCreativeModeTabItemsPacket.create(serverPlayer));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;

        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        // prevent duplicated interact air events
        serverPlayer.updateLastInteractEntityTick(hand);

        Item itemInHand = serverPlayer.getItemInHand(hand);

        if (ItemUtils.isEmpty(itemInHand)) return;
        Optional<ItemDefinition> optionalCustomItem = itemInHand.getDefinition();
        if (optionalCustomItem.isEmpty()) return;
        // 如果目标实体与手中物品可以产生交互，那么忽略
        if (InteractUtils.isEntityInteractable(player, entity, itemInHand)) return;

        Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
        BukkitEntity bukkitEntity = new BukkitEntity(entity);
        PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand)
                .withParameter(DirectContextParameters.HAND, hand)
                .withParameter(DirectContextParameters.EVENT, cancellable)
                .withParameter(DirectContextParameters.ENTITY, bukkitEntity)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(event.getRightClicked().getLocation()))
        );
        ItemDefinition itemDefinition = optionalCustomItem.get();
        itemDefinition.execute(context, EventTrigger.RIGHT_CLICK);
        if (event.isCancelled()) return;

        Optional<ItemBehavior> optionalItemBehavior = itemInHand.getBehavior();
        if (optionalItemBehavior.isEmpty()) return;

        Location entityLocation = entity.getLocation();
        Vec3d hitLocation;
        if (event instanceof PlayerInteractAtEntityEvent atEvent) {
            org.bukkit.util.Vector clicked = atEvent.getClickedPosition();
            hitLocation = new Vec3d(entityLocation.getX() + clicked.getX(), entityLocation.getY() + clicked.getY(), entityLocation.getZ() + clicked.getZ());
        } else {
            hitLocation = new Vec3d(entityLocation.getX(), entityLocation.getY() + entity.getHeight() / 2, entityLocation.getZ());
        }
        Location eyeLocation = player.getEyeLocation();
        Direction direction = Direction.getApproximateNearest(
                eyeLocation.getX() - hitLocation.x,
                eyeLocation.getY() - hitLocation.y,
                eyeLocation.getZ() - hitLocation.z
        );
        InteractEntityContext interactEntityContext = new InteractEntityContext(serverPlayer, hand, new EntityHitResult(direction, hitLocation), bukkitEntity);
        InteractionResult useResult = optionalItemBehavior.get().useOnEntity(interactEntityContext);
        if (useResult.success()) {
            serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
        }
        if (useResult == InteractionResult.SUCCESS_AND_CANCEL) {
            event.setCancelled(true);
        }
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

        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        // 如果本tick内主手已被处理，则不处理副手
        // 这是因为客户端可能会同时发主副手交互包，但实际上只能处理其中一个
        if (serverPlayer.hasInteractionInThisTick()) {
            event.setCancelled(true);
            return;
        }

        // 这是模拟执行的逻辑，且只会同手执行，所以出现这个情况需要直接返回，避免递归调用和二次触发函数
        if (serverPlayer.isSimulatingInteraction()) {
            return;
        }

        // some common data
        Block block = Objects.requireNonNull(event.getClickedBlock());
        BlockData blockData = block.getBlockData();
        Object blockState = BlockStateUtils.blockDataToBlockState(blockData);
        ImmutableBlockState immutableBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        Item itemInHand = serverPlayer.getItemInHand(hand);
        Location interactionPoint = EventUtils.getInteractionPoint(event);

        BlockHitResult hitResult = null;
        if (action == Action.RIGHT_CLICK_BLOCK && interactionPoint != null) {
            Direction direction = DirectionUtils.toDirection(event.getBlockFace());
            BlockPos pos = LocationUtils.toBlockPos(block.getLocation());
            Vec3d vec3d = new Vec3d(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            hitResult = new BlockHitResult(vec3d, direction, pos, false); // todo 需要检测玩家是否在方块内，特指脚手架
        }
        BukkitWorld world = BukkitAdaptor.adapt(block.getWorld());

        // 处理自定义方块
        if (immutableBlockState != null) {
            // call the event if it's custom
            ContextHolder.Builder contextBuilder = ContextHolder.builder();
            CustomBlockInteractEvent interactEvent = new CustomBlockInteractEvent(
                    player, block.getLocation(), interactionPoint, immutableBlockState,
                    block, event.getBlockFace(), hand,
                    action == Action.RIGHT_CLICK_BLOCK ? CustomBlockInteractEvent.Action.RIGHT_CLICK : CustomBlockInteractEvent.Action.LEFT_CLICK,
                    event.getItem(), contextBuilder
            );
            if (EventUtils.fireAndCheckCancel(interactEvent)) {
                event.setCancelled(true);
                return;
            }

            // fix client side issues
            if (action == Action.RIGHT_CLICK_BLOCK && hitResult != null &&
                    InteractUtils.canPlaceVisualBlock(player, BlockStateUtils.fromBlockData(immutableBlockState.visualBlockState().minecraftState()), hitResult, itemInHand)) {
                player.updateInventory();
            }

            Cancellable dummy = Cancellable.dummy();
            // run custom functions
            BlockDefinition blockDefinition = immutableBlockState.owner().value();
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, contextBuilder
                    .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                    .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, immutableBlockState)
                    .withParameter(DirectContextParameters.HAND, hand)
                    .withParameter(DirectContextParameters.EVENT, dummy)
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                    .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand)
            );
            if (action == Action.RIGHT_CLICK_BLOCK) blockDefinition.execute(context, EventTrigger.RIGHT_CLICK);
            else blockDefinition.execute(context, EventTrigger.LEFT_CLICK);
            if (dummy.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            // 事件里已经有交互了
            if (serverPlayer.hasInteractionInThisTick()) {
                return;
            }

            if (hitResult != null) {
                UseOnContext useOnContext = new UseOnContext(world, serverPlayer, hand, itemInHand, hitResult);
                boolean hasItem = !serverPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() || !serverPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty();
                boolean flag = player.isSneaking() && hasItem;
                if (!flag) {
                    if (immutableBlockState.behavior() instanceof BlockBehavior behavior) {
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
                Key blockOwner = BlockStateUtils.getBlockOwnerIdFromState(blockState);
                if (this.plugin.blockManager().isInteractSoundMissing(blockOwner)) {
                    boolean hasItem = !serverPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() || !serverPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty();
                    boolean flag = player.isSneaking() && hasItem;
                    if (!flag) {
                        if (blockData instanceof Openable openable) {
                            SoundSet soundSet = SoundSet.getByBlock(blockOwner);
                            if (soundSet != null) {
                                serverPlayer.playSound(
                                        Vec3d.atCenterOf(hitResult.blockPos()),
                                        openable.isOpen() ? soundSet.closeSound() : soundSet.openSound(),
                                        SoundSource.BLOCK,
                                        1, RandomUtils.generateRandomFloat(0.9f, 1)
                                );
                            }
                        } else if (blockData instanceof Powerable powerable && !powerable.isPowered()) {
                            SoundSet soundSet = SoundSet.getByBlock(blockOwner);
                            if (soundSet != null) {
                                serverPlayer.playSound(
                                        Vec3d.atCenterOf(hitResult.blockPos()),
                                        soundSet.openSound(),
                                        SoundSource.BLOCK,
                                        1, RandomUtils.generateRandomFloat(0.9f, 1)
                                );
                            }
                        }
                    }
                }
            }
        }

        boolean hasItem = !itemInHand.isEmpty();
        Optional<ItemDefinition> optionalItemDefinition = hasItem ? itemInHand.getDefinition() : Optional.empty();
        boolean isCustomItem = optionalItemDefinition.isPresent() && !optionalItemDefinition.get().isVanillaItem();

        // interact block with items
        if (hasItem && action == Action.RIGHT_CLICK_BLOCK) {
            // some plugins would trigger this event without interaction point
            if (interactionPoint == null) {
                if (isCustomItem) {
                    event.setCancelled(true);
                }
                return;
            }

            // 如果手中物品在原版是可以放出方块的物品
            boolean canPlaceBlock = false;
            if (itemInHand.isBlockItem()) {
                // 它也确实是原版物品
                if (!isCustomItem) {
                    // 它目前可以被放置出来
                    if (InteractUtils.canPlaceBlock(new BlockPlaceContext(new UseOnContext(world, serverPlayer, hand, itemInHand, hitResult)))) {
                        // 如果交互目标是一个自定义方块
                        if (immutableBlockState != null) {
                            // 如果客户端觉得它可交互，那么就不会意淫出声音
                            BlockData craftBlockData = BlockStateUtils.fromBlockData(immutableBlockState.visualBlockState().minecraftState());
                            if (InteractUtils.isInteractable(player, craftBlockData, hitResult, itemInHand)) {
                                if (!serverPlayer.isSecondaryUseActive()) {
                                    serverPlayer.setResendSound();
                                }
                            } else {
                                // 如果服务端侧可替换，但是客户端觉得不行，就要重新挥手
                                if (BlockStateUtils.isReplaceable(immutableBlockState.customBlockState().minecraftState()) && !BlockStateUtils.isReplaceable(immutableBlockState.visualBlockState().minecraftState())) {
                                    serverPlayer.setResendSwing();
                                }
                            }
                        }
                        canPlaceBlock = true;
                    }
                }
                // 是自定义物品，尝试禁用掉原版放置逻辑（前提是能放）
                else {
                    if (optionalItemDefinition.get().settings().disableVanillaBehavior()) {
                        // 不能在BlockPlaceEvent里检测，是因为种农作物不触发相关事件
                        // 允许尝试放置方块
                        if (serverPlayer.isSecondaryUseActive() || !InteractUtils.isInteractable(player, blockData, hitResult, itemInHand)) {
                            if (InteractUtils.canPlaceBlock(new BlockPlaceContext(new UseOnContext(world, serverPlayer, hand, itemInHand, hitResult)))) {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }

            // 事件里已经有交互了
            if (serverPlayer.hasInteractionInThisTick()) {
                return;
            }

            // 优先检查物品行为，再执行自定义事件
            // 检查其他的物品行为，物品行为理论只在交互时处理
            Optional<ItemBehavior> optionalItemBehavior = itemInHand.getBehavior();
            // 物品类型是否包含自定义物品行为，行为不一定来自于自定义物品，部分原版物品也包含了新的行为
            if (optionalItemBehavior.isPresent()) {
                // 检测是否可交互应当只判断原版方块，因为自定义方块早就判断过了，如果可交互不可能到这一步
                boolean interactable = immutableBlockState == null && InteractUtils.isInteractable(player, blockData, hitResult, itemInHand);
                // 如果方块可交互但是玩家没shift，那么原版的方块交互优先，取消自定义物品的behavior
                // todo 如果我的物品行为允许某些交互呢？是否值得进一步处理？
                if (!serverPlayer.isSecondaryUseActive() && interactable) {
                    return;
                }
                UseOnContext useOnContext = new UseOnContext(world, serverPlayer, hand, itemInHand, hitResult);
                // 依次执行物品行为
                InteractionResult useResult = optionalItemBehavior.get().useOnBlock(useOnContext);
                if (useResult.success()) {
                    serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                }
                if (useResult == InteractionResult.SUCCESS_AND_CANCEL) {
                    event.setCancelled(true);
                    return;
                }
                if (useResult != InteractionResult.PASS) {
                    return;
                }
            }

            // 执行物品右键事件
            if (isCustomItem) {
                // 要求服务端侧这个方块不可交互，或玩家处于潜行状态
                if (serverPlayer.isSecondaryUseActive() || !InteractUtils.isInteractable(player, blockData, hitResult, itemInHand)) {
                    Cancellable dummy = Cancellable.dummy();
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                            .withOptionalParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, immutableBlockState)
                            .withParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                            .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                            .withParameter(DirectContextParameters.HAND, hand)
                            .withParameter(DirectContextParameters.EVENT, dummy)
                    );
                    ItemDefinition itemDefinition = optionalItemDefinition.get();
                    itemDefinition.execute(context, EventTrigger.RIGHT_CLICK);
                    if (dummy.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            // 事件里完成交互
            if (serverPlayer.hasInteractionInThisTick()) {
                return;
            }

            // 客户端觉得方块自定义可交互，可实际上未交互。这时候客户端只会发一个主手交互包
            if (immutableBlockState != null  // 必须是自定义方块才能触发
                    && !serverPlayer.isSecondaryUseActive()  // 没有shift
                    && !canPlaceBlock  // 物品不可放置
                    && InteractUtils.isInteractable(player, BlockStateUtils.fromBlockData(immutableBlockState.visualBlockState().minecraftState()), hitResult, itemInHand)) {
                // 首先得允许使用手中物品，副手逻辑也可能到这里
                if (event.useItemInHand() != Event.Result.DENY) {
                    event.setUseItemInHand(Event.Result.DENY);
                    Object nmsHitResult = InteractUtils.toNMSHitResult(hitResult);
                    Object item = ItemStackProxy.INSTANCE.getItem(itemInHand.minecraftItem());
                    Object result;
                    try {
                        serverPlayer.setIsSimulatingInteraction(true);
                        // 先尝试 useOn
                        result = ItemProxy.INSTANCE.useOn(item, UseOnContextProxy.INSTANCE.newInstance(
                                world.minecraftWorld(),
                                serverPlayer.serverPlayer(),
                                hand == InteractionHand.MAIN_HAND ? InteractionHandProxy.MAIN_HAND : InteractionHandProxy.OFF_HAND,
                                itemInHand.minecraftItem(),
                                nmsHitResult
                        ));
                        if (result != InteractionResultProxy.INSTANCE.getPass()) {
                            return;
                        }
                        // 再尝试 use
                        result = ItemProxy.INSTANCE.use(
                                item,
                                serverPlayer.world().minecraftWorld(),
                                serverPlayer.serverPlayer(),
                                hand == InteractionHand.MAIN_HAND ? InteractionHandProxy.MAIN_HAND : InteractionHandProxy.OFF_HAND
                        );
                    } finally {
                        serverPlayer.setIsSimulatingInteraction(false);
                    }

                    // 最后尝试副手
                    if (hand == InteractionHand.MAIN_HAND && (result == InteractionResultProxy.INSTANCE.getFail() || result == InteractionResultProxy.INSTANCE.getPass())) {
                        serverPlayer.simulatePacket(ServerboundUseItemOnPacketProxy.INSTANCE.newInstance(
                                InteractionHandProxy.OFF_HAND,
                                nmsHitResult,
                                0
                        ));
                    }
                }
            }
        }

        // 主手没物品，但是副手有物品，客户端觉得此方块可交互，漏副手包
        if (!hasItem && hand == InteractionHand.MAIN_HAND && hitResult != null && immutableBlockState != null) {
            if (!serverPlayer.isSecondaryUseActive() && InteractUtils.isInteractable(player, BlockStateUtils.fromBlockData(immutableBlockState.visualBlockState().minecraftState()), hitResult, itemInHand)) {
                serverPlayer.simulatePacket(ServerboundUseItemOnPacketProxy.INSTANCE.newInstance(
                        InteractionHandProxy.OFF_HAND,
                        InteractUtils.toNMSHitResult(hitResult),
                        0
                ));
            }
        }

        // 执行物品左键事件
        if (isCustomItem && action == Action.LEFT_CLICK_BLOCK) {
            Cancellable dummy = Cancellable.dummy();
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                    .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                    .withOptionalParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, immutableBlockState)
                    .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand)
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                    .withParameter(DirectContextParameters.HAND, hand)
            );
            ItemDefinition itemDefinition = optionalItemDefinition.get();
            itemDefinition.execute(context, EventTrigger.LEFT_CLICK);
            if (dummy.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAir(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.LEFT_CLICK_AIR)
            return;
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null || serverPlayer.isSpectatorMode()) {
            return;
        }
        // Gets the item in hand
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        // prevents duplicated events
        if (serverPlayer.lastInteractEntityCheck(hand)) {
            return;
        }

        Item itemInHand = serverPlayer.getItemInHand(hand);
        // should never be null
        if (itemInHand.isEmpty()) return;

        Optional<ItemDefinition> optionalCustomItem = itemInHand.getDefinition();
        if (optionalCustomItem.isPresent()) {
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                    .withParameter(DirectContextParameters.HAND, hand)
                    .withParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(player.getLocation()))
            );
            ItemDefinition itemDefinition = optionalCustomItem.get();
            if (action == Action.RIGHT_CLICK_AIR) itemDefinition.execute(context, EventTrigger.RIGHT_CLICK);
            else itemDefinition.execute(context, EventTrigger.LEFT_CLICK);
        }

        // 事件里已经有交互了
        if (serverPlayer.hasInteractionInThisTick()) {
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR) {
            Optional<ItemBehavior> optionalItemBehavior = itemInHand.getBehavior();
            if (optionalItemBehavior.isPresent()) {
                InteractionResult useResult = optionalItemBehavior.get().use(serverPlayer.world(), serverPlayer, hand);
                if (useResult.success()) {
                    serverPlayer.updateLastSuccessfulInteractionTick(serverPlayer.gameTicks());
                }
                if (useResult == InteractionResult.SUCCESS_AND_CANCEL) {
                    event.setCancelled(true);
                    return;
                }
                if (useResult != InteractionResult.PASS) {
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        ItemStack consumedItem = event.getItem();
        if (ItemStackUtils.isEmpty(consumedItem)) return;
        Item wrapped = this.plugin.itemManager().wrap(consumedItem);
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
        ItemDefinition itemDefinition = optionalCustomItem.get();
        PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                .withParameter(DirectContextParameters.ITEM_IN_HAND, wrapped)
                .withParameter(DirectContextParameters.EVENT, cancellable)
                .withParameter(DirectContextParameters.HAND, event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)
        );
        itemDefinition.execute(context, EventTrigger.CONSUME);
        if (event.isCancelled()) {
            return;
        }
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            Key replacement = itemDefinition.settings().consumeReplacement();
            if (wrapped.count() == 1) {
                if (replacement != null && VersionHelper.hasPaperPatch) {
                    BukkitItem replacementItem = (BukkitItem) Item.byId(replacement, serverPlayer);
                    if (replacementItem != null) {
                        event.setReplacement(replacementItem.getBukkitItem());
                    }
                }
            } else {
                // fixme 如何取消堆叠数量>1的物品的默认replacement
                if (replacement != null) {
                    Item replacementItem = Item.byId(replacement, serverPlayer);
                    if (replacementItem != null) {
                        PlayerUtils.giveItem(serverPlayer, 1, replacementItem, false);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (VersionHelper.isOrAbove1_20_5) return;
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack consumedItem = event.getItem();
        if (ItemStackUtils.isEmpty(consumedItem)) return;
        Item wrapped = this.plugin.itemManager().wrap(consumedItem);
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) {
            return;
        }
        ItemDefinition itemDefinition = optionalCustomItem.get();
        FoodData foodData = itemDefinition.settings().foodData();
        if (foodData == null) return;
        event.setCancelled(true);
        int oldFoodLevel = player.getFoodLevel();
        int newFoodLevel = MiscUtils.clamp(oldFoodLevel + foodData.nutrition(), 0, 20);
        if (foodData.nutrition() != 0) player.setFoodLevel(newFoodLevel);
        float oldSaturation = player.getSaturation();
        if (foodData.saturation() != 0) player.setSaturation(MiscUtils.clamp(oldSaturation + foodData.saturation(), 0, newFoodLevel));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Item item) {
            Optional.of(this.plugin.itemManager().wrap(item.getItemStack()))
                    .flatMap(Item::getDefinition)
                    .ifPresent(it -> {
                        if (it.settings().invulnerable().contains(DamageCauseUtils.fromBukkit(event.getCause()))) {
                            event.setCancelled(true);
                        }
                    });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttackEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            Entity hitEntity = event.getEntity();
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
            if (serverPlayer == null || serverPlayer.isSpectatorMode()) return;

            // 获取物品
            BukkitItem itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
            if (ItemUtils.isEmpty(itemInHand)) return;
            Optional<ItemDefinition> optionalCustomItem = itemInHand.getDefinition();
            if (optionalCustomItem.isEmpty()) return;

            // 触发事件
            Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
            PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                    .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                    .withParameter(DirectContextParameters.EVENT, cancellable)
                    .withParameter(DirectContextParameters.ENTITY, new BukkitEntity(hitEntity))
                    .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(hitEntity.getLocation()))
            );
            ItemDefinition itemDefinition = optionalCustomItem.get();
            itemDefinition.execute(context, EventTrigger.ATTACK);
        }
    }

    // 禁止附魔
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEnchant(PrepareItemEnchantEvent event) {
        ItemStack itemToEnchant = event.getItem();
        Item wrapped = this.plugin.itemManager().wrap(itemToEnchant);
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) return;
        ItemDefinition itemDefinition = optionalCustomItem.get();
        if (!itemDefinition.settings().canEnchant()) {
            event.setCancelled(true);
        }
    }

    // 用于附魔台纠正
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof EnchantingInventory inventory)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack lazuli = inventory.getSecondary();
        if (lazuli != null) return;
        ItemStack item = inventory.getItem();
        if (ItemStackUtils.isEmpty(item)) return;
        Item wrapped = this.plugin.itemManager().wrap(item);
        if (ItemUtils.isEmpty(wrapped)) return;
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) return;
        BukkitItemDefinition customItem = (BukkitItemDefinition) optionalCustomItem.get();
        if (customItem.clientItem() == ItemStackProxy.INSTANCE.getItem(wrapped.minecraftItem())) return;
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        this.plugin.scheduler().platform().runDelayed(() -> {
            Object container = PlayerProxy.INSTANCE.getContainerMenu(serverPlayer.serverPlayer());
            if (!EnchantmentMenuProxy.CLASS.isInstance(container)) return;
            Object secondSlotItem = SlotProxy.INSTANCE.getItem(AbstractContainerMenuProxy.INSTANCE.getSlot(container, 1));
            if (secondSlotItem == null || ItemStackProxy.INSTANCE.isEmpty(secondSlotItem)) return;
            Object[] dataSlots = AbstractContainerMenuProxy.INSTANCE.getDataSlots(container).toArray();
            List<Object> packets = new ArrayList<>(dataSlots.length);
            for (int i = 0; i < dataSlots.length; i++) {
                Object dataSlot = dataSlots[i];
                int data = DataSlotProxy.INSTANCE.get(dataSlot);
                packets.add(ClientboundContainerSetDataPacketProxy.INSTANCE.newInstance(AbstractContainerMenuProxy.INSTANCE.getContainerId(container), i, data));
            }
            serverPlayer.sendPackets(packets, false);
        }, null, serverPlayer.platformPlayer());
    }

    /*
    左键修复到耐久最大值或用尽材料，右键只消耗单个材料。
    目标已修满或无法修复时不拦截事件，保持原版交换行为。
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDragRepairItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != player.getInventory()) return;
        org.bukkit.event.inventory.ClickType clickType = event.getClick();
        if (clickType != org.bukkit.event.inventory.ClickType.LEFT && clickType != org.bukkit.event.inventory.ClickType.RIGHT) return;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (ItemStackUtils.isEmpty(cursor) || ItemStackUtils.isEmpty(current)) return;
        Item wrappedMaterial = this.itemManager.wrap(cursor);
        Optional<ItemDefinition> optionalMaterial = wrappedMaterial.getDefinition();
        if (optionalMaterial.isEmpty()) return;
        List<DragRepairItem> dragRepairItems = optionalMaterial.get().settings().dragRepairItems();
        if (dragRepairItems.isEmpty()) return;
        Item wrappedTarget = this.itemManager.wrap(current);
        int maxDamage = wrappedTarget.maxDamage();
        int damage = wrappedTarget.damage().orElse(0);
        // 目标无耐久或已修满
        if (maxDamage <= 0 || damage <= 0) return;
        Optional<ItemDefinition> optionalTarget = wrappedTarget.getDefinition();
        Key targetId = wrappedTarget.id();
        DragRepairItem repairItem = null;
        for (DragRepairItem item : dragRepairItems) {
            for (String target : item.targets()) {
                if (target.charAt(0) == '#') {
                    Key tag = Key.of(target.substring(1));
                    if (optionalTarget.isPresent() && optionalTarget.get().is(tag)) {
                        repairItem = item;
                        break;
                    }
                    if (wrappedTarget.hasVanillaTag(tag)) {
                        repairItem = item;
                        break;
                    }
                } else if (target.equals(targetId.toString())) {
                    repairItem = item;
                    break;
                }
            }
            if (repairItem != null) break;
        }
        // 找不到匹配的修复目标
        if (repairItem == null) return;
        int durabilityPerItem = (int) (repairItem.amount() + repairItem.percent() * maxDamage);
        if (durabilityPerItem <= 0) return;
        int consumeAmount = clickType == org.bukkit.event.inventory.ClickType.LEFT
                ? Math.min((damage + durabilityPerItem - 1) / durabilityPerItem, wrappedMaterial.count())
                : 1;
        event.setCancelled(true);
        wrappedTarget.damage(Math.max(damage - consumeAmount * durabilityPerItem, 0));
        event.setCurrentItem(ItemStackUtils.getBukkitStack(wrappedTarget.minecraftItem()));
        wrappedMaterial.shrink(consumeAmount);
        player.setItemOnCursor(wrappedMaterial.count() <= 0 ? null : ItemStackUtils.getBukkitStack(wrappedMaterial.minecraftItem()));
        SoundData sound = repairItem.sound();
        if (sound != null) {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
            if (serverPlayer != null) {
                serverPlayer.playSound(sound.id(), SoundSource.PLAYER, sound.volume().get(), sound.pitch().get());
            }
        }
    }

    /*

    关于物品更新器

     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(event.getPlayer());
        if (serverPlayer == null) return;
        serverPlayer.stopMiningBlock();
        if (!Config.triggerUpdateDrop()) return;
        org.bukkit.entity.Item itemDrop = event.getItemDrop();
        ItemStack itemStack = itemDrop.getItemStack();
        Item wrapped = this.itemManager.wrap(itemStack);
        ItemUpdateResult result = this.itemManager.updateItem(wrapped, () -> ItemBuildContext.of(serverPlayer));
        if (result.updated()) {
            itemDrop.setItemStack(ItemStackUtils.getBukkitStack(result.finalItem().minecraftItem()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPickUpItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        org.bukkit.entity.Item itemDrop = event.getItem();
        ItemStack itemStack = itemDrop.getItemStack();
        Item wrapped = this.itemManager.wrap(itemStack);
        // 低版本拙劣的inventory change替代品
        if (!VersionHelper.isOrAbove1_20_3) {
            this.itemManager.unlockRecipeOnInventoryChanged(player, wrapped);
        }
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) return;
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        ItemDefinition itemDefinition = optionalCustomItem.get();
        if (Config.triggerUpdatePickUp() && itemDefinition.updater().isPresent()) {
            ItemUpdateResult result = this.itemManager.updateItem(wrapped, () -> ItemBuildContext.of(serverPlayer));
            if (result.updated()) {
                itemDrop.setItemStack(ItemStackUtils.getBukkitStack(result.finalItem().minecraftItem()));
            }
        }
        Cancellable dummy = Cancellable.dummy();
        itemDefinition.execute(PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                .withParameter(DirectContextParameters.ENTITY, BukkitAdaptor.adapt(itemDrop))
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(itemDrop.getLocation()))
                .withParameter(DirectContextParameters.EVENT, dummy)
        ), EventTrigger.PICK_UP);
        if (dummy.isCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryClickItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clickedInventory = event.getClickedInventory();
        // 点击自己物品栏里的物品
        if (clickedInventory == null || clickedInventory != player.getInventory()) return;
        ItemStack currentItem = event.getCurrentItem();
        Item wrapped = this.itemManager.wrap(currentItem);
        // 低版本拙劣的inventory change替代品
        if (!VersionHelper.isOrAbove1_20_3) {
            this.itemManager.unlockRecipeOnInventoryChanged(player, wrapped);
        }
        if (Config.triggerUpdateClick()) {
            ItemUpdateResult result = this.itemManager.updateItem(wrapped, () -> ItemBuildContext.of(BukkitAdaptor.adapt(player)));
            if (!result.updated() || !result.replaced()) {
                return;
            }
            event.setCurrentItem(ItemStackUtils.getBukkitStack(result.finalItem().minecraftItem()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        serverPlayer.stopMiningBlock();
    }

    @SuppressWarnings("DuplicatedCode")
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 依赖 paper api实现
        if (!VersionHelper.hasPaperPatch) return;

        BukkitItemManager instance = BukkitItemManager.instance();

        // 处理损毁物品
        if (event.getKeepInventory()) {
            if (!instance.featureFlag$destroyOnDeathChance()) return;

            Random random = ThreadLocalRandom.current();
            PlayerInventory inventory = event.getPlayer().getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (item == null) continue;

                Optional<ItemDefinition> optional = instance.wrap(item).getDefinition();
                if (optional.isEmpty()) continue;

                ItemDefinition itemDefinition = optional.get();
                ItemSettings settings = itemDefinition.settings();
                float destroyChance = settings.destroyOnDeathChance();
                if (destroyChance <= 0f) continue;

                int totalAmount = item.getAmount();
                int destroyCount = 0;

                for (int i = 0; i < totalAmount; i++) {
                    float rand = random.nextFloat();
                    // 判断是否损毁
                    if (destroyChance > 0f && rand < destroyChance) {
                        destroyCount++;
                    }
                }
                if (destroyCount != 0) {
                    item.setAmount(totalAmount - destroyCount);
                }
            }
        }
        // 处理保留 + 损毁物品
        else {
            if (!instance.featureFlag$keepOnDeathChance() && !instance.featureFlag$destroyOnDeathChance()) return;
            Random random = ThreadLocalRandom.current();

            List<ItemStack> itemsToKeep = event.getItemsToKeep();
            List<ItemStack> itemsToDrop = event.getDrops();

            Iterator<ItemStack> iterator = itemsToDrop.iterator();

            while (iterator.hasNext()) {
                ItemStack item = iterator.next();
                Optional<ItemDefinition> optional = instance.wrap(item).getDefinition();
                if (optional.isEmpty()) continue;

                ItemDefinition itemDefinition = optional.get();
                ItemSettings settings = itemDefinition.settings();

                float destroyChance = settings.destroyOnDeathChance();
                float keepChance = settings.keepOnDeathChance();

                // 如果没有效果，跳过
                if (destroyChance <= 0f && keepChance <= 0f) continue;

                int totalAmount = item.getAmount();

                int keepCount = 0;
                int destroyCount = 0;
                int dropCount = 0;

                for (int i = 0; i < totalAmount; i++) {
                    float rand = random.nextFloat();

                    // 先判断是否损毁
                    if (destroyChance > 0f && rand < destroyChance) {
                        destroyCount++;
                    }
                    // 然后判断是否保留（在未损毁的物品中）
                    else if (keepChance > 0f && rand < (destroyChance + keepChance)) {
                        keepCount++;
                    }
                    // 否则掉落
                    else {
                        dropCount++;
                    }
                }

                // 处理结果
                if (destroyCount == totalAmount) {
                    iterator.remove();
                    continue;
                }

                if (keepCount == 0 && dropCount == 0) {
                    // 实际上不会发生这种情况
                    continue;
                }

                if (keepCount > 0) {
                    ItemStack keepItem = item.clone();
                    keepItem.setAmount(keepCount);
                    itemsToKeep.add(keepItem);
                }

                if (dropCount > 0) {
                    item.setAmount(dropCount);
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onShootBow(EntityShootBowEvent event) {
        LivingEntity shooter = event.getEntity();
        ItemStack bow = event.getBow();
        BukkitItem bowItem = this.itemManager.wrap(bow);
        BukkitServerPlayer serverPlayer = shooter instanceof Player player ? BukkitAdaptor.adapt(player) : null;

        // 触发射击事件
        bowItem.getDefinition().ifPresent(definition -> {
            definition.execute(PlayerOptionalContext.of(serverPlayer,
                    ContextHolder.builder()
                            .withParameter(DirectContextParameters.EVENT, Cancellable.of(event::isCancelled, event::setCancelled))
                            .withParameter(DirectContextParameters.ENTITY, new BukkitEntity(shooter))
                            .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(shooter.getLocation()))
                            .withParameter(DirectContextParameters.ITEM_IN_HAND, bowItem)
            ), EventTrigger.SHOOT);
        });

        ItemStack consumable = event.getConsumable();
        if (consumable == null) {
            return;
        }
        BukkitItem arrowItem = this.itemManager.wrap(consumable);

        // 替换弹射物
        Entity projectile = event.getProjectile();
        Key weaponId = bowItem.vanillaId();
        boolean replaceProjectile = false;
        if (weaponId.equals(ItemKeys.BOW)) {
            replaceProjectile = !this.itemManager.isBowAmmo(arrowItem);
        } else if (weaponId.equals(ItemKeys.CROSSBOW)) {
            replaceProjectile = !this.itemManager.isCrossbowAmmo(arrowItem);
        }
        if (replaceProjectile) {
            Projectile projectileByItem = ProjectileItems.createProjectileByItem(projectile.getLocation(), arrowItem, shooter, projectile instanceof AbstractArrow abstractArrow && abstractArrow.isCritical());
            if (projectileByItem != null) {
                projectileByItem.setVelocity(projectile.getVelocity());
                event.setProjectile(projectileByItem);
                projectile = projectileByItem;
            }
        }

        // 设置一些其他属性，无限和是否允许捡起
        Optional<ItemDefinition> arrowDefinition = arrowItem.getDefinition();
        if (arrowDefinition.isPresent()) {
            ItemDefinition definition = arrowDefinition.get();
            ProjectileMeta projectileMeta = definition.settings().projectileMeta();
            if (projectileMeta != null && serverPlayer != null && !serverPlayer.isCreativeMode()) {
                if (projectileMeta.ignoreInfinityEnchantment() && bowItem.getEnchantment(EnchantmentKeys.INFINITY).isPresent()) {
                    serverPlayer.clearOrCountMatchingInventoryItems(arrowItem.id(), 1);
                    if (projectile instanceof AbstractArrow p1 && projectileMeta.pickupable()) {
                        p1.setPickupStatus(AbstractArrow.PickupStatus.ALLOWED);
                        BukkitItem arrow = this.itemManager.wrap(p1.getItemStack());
                        arrow.removeComponent(DataComponentKeys.INTANGIBLE_PROJECTILE);
                        p1.setItemStack(arrow.getBukkitItem());
                    }
                }
                if (!projectileMeta.pickupable() && projectile instanceof AbstractArrow p1) {
                    p1.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    BukkitItem arrow = this.itemManager.wrap(p1.getItemStack());
                    arrow.setJavaComponent(DataComponentKeys.INTANGIBLE_PROJECTILE, Map.of());
                    p1.setItemStack(arrow.getBukkitItem());
                }
            }
        }
    }
}
