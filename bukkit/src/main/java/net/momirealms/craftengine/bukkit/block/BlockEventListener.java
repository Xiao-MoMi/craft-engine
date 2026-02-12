package net.momirealms.craftengine.bukkit.block;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class BlockEventListener implements Listener {
    private final BukkitCraftEngine plugin;
    private final BukkitBlockManager manager;

    public BlockEventListener(BukkitCraftEngine plugin, BukkitBlockManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!VersionHelper.isOrAbove1_20_5()) {
            if (event.getDamager() instanceof Player player) {
                BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
                if (serverPlayer == null) return;
                serverPlayer.setClientSideCanBreakBlock(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceBlock(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return;
        // send swing if player is clicking a replaceable block
        if (serverPlayer.shouldResendSwing()) {
            player.swingHand(event.getHand());
        }
        // send sound if the placed block's sounds are removed
        if (Config.enableSoundSystem()) {
            Block block = event.getBlock();
            Object blockState = BlockStateUtils.getBlockState(block);
            if (blockState != MBlocks.AIR$defaultState && BlockStateUtils.isVanillaBlock(blockState)) {
                Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
                Object soundEvent = FastNMS.INSTANCE.field$SoundType$placeSound(soundType);
                Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
                if (this.manager.isPlaceSoundMissing(soundId)) {
                    if (player.getInventory().getItemInMainHand().getType() != Material.DEBUG_STICK) {
                        player.playSound(block.getLocation().add(0.5, 0.5, 0.5), soundId.toString(), SoundCategory.BLOCKS, 1f, 0.8f);
                    }
                    return;
                }
            }
        }
        // resend sound if the clicked block is interactable on client side
        if (serverPlayer.shouldResendSound()) {
            Block block = event.getBlock();
            Object blockState = BlockStateUtils.getBlockState(block);
            Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
            Object soundEvent = FastNMS.INSTANCE.field$SoundType$placeSound(soundType);
            Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
            player.playSound(block.getLocation().add(0.5, 0.5, 0.5), soundId.toString(), SoundCategory.BLOCKS, 1f, 0.8f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBreak(BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        Object blockState = BlockStateUtils.getBlockState(block);
        int stateId = BlockStateUtils.blockStateToId(blockState);
        Player player = event.getPlayer();
        Location location = block.getLocation();
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return;
        serverPlayer.updateLastSuccessBreakTick();
        net.momirealms.craftengine.core.world.World world = BukkitAdaptors.adapt(player.getWorld());
        BlockPos blockPos = LocationUtils.toBlockPos(location);
        WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        Item<ItemStack> itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (!event.isCancelled() && !ItemUtils.isEmpty(itemInHand)) {
            Optional<CustomItem<ItemStack>> optionalCustomItem = itemInHand.getCustomItem();
            if (optionalCustomItem.isPresent()) {
                CustomItem<ItemStack> customItem = optionalCustomItem.get();
                Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
                customItem.execute(
                        PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                        ), EventTrigger.BREAK
                );
                if (cancellable.isCancelled()) {
                    return;
                }
                for (ItemBehavior behavior : customItem.behaviors()) {
                    behavior.breakBlock(world, serverPlayer, blockPos);
                }
            }
        }

        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            ImmutableBlockState state = this.manager.getImmutableBlockStateUnsafe(stateId);
            if (!state.isEmpty()) {
                if (!event.isCancelled()) {
                    // double check adventure mode to prevent dupe
                    if (!FastNMS.INSTANCE.field$Player$mayBuild(serverPlayer.serverPlayer()) && !serverPlayer.canBreak(blockPos, null)) {
                        return;
                    }

                    // trigger api event
                    ContextHolder.Builder contextBuilder = ContextHolder.builder();
                    CustomBlockBreakEvent customBreakEvent = new CustomBlockBreakEvent(serverPlayer, location, block, state, event.isDropItems(), contextBuilder);
                    boolean isCancelled = EventUtils.fireAndCheckCancel(customBreakEvent);
                    if (isCancelled) {
                        event.setCancelled(true);
                        return;
                    }

                    // 同步选项
                    event.setDropItems(customBreakEvent.dropItems());

                    // execute functions
                    Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, contextBuilder
                            .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                            .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, state)
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, ItemUtils.isEmpty(itemInHand) ? null : itemInHand)
                    );
                    state.owner().value().execute(context, EventTrigger.BREAK);
                    if (cancellable.isCancelled()) {
                        return;
                    }

                    // play sound
                    serverPlayer.playSound(position, state.settings().sounds().breakSound(), SoundSource.BLOCK);
                }
                // Restore sounds in cancelled events
                else {
                    if (Config.processCancelledBreak()) {
                        if (BukkitItemUtils.isDebugStick(itemInHand)) return;
                        serverPlayer.playSound(position, state.settings().sounds().breakSound(), SoundSource.BLOCK);
                    }
                }
            }
        } else {
            // override vanilla block loots
            if (!event.isCancelled() && player.getGameMode() != GameMode.CREATIVE) {
                this.plugin.vanillaLootManager().getBlockLoot(stateId).ifPresent(it -> {
                    if (!event.isDropItems()) {
                        return;
                    }
                    if (it.override()) {
                        event.setDropItems(false);
                        event.setExpToDrop(0);
                    }
                    ContextHolder lootContext = ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, ItemUtils.isEmpty(itemInHand) ? null : itemInHand).build();
                    for (LootTable<?> lootTable : it.lootTables()) {
                        for (Item<?> item : lootTable.getRandomItems(lootContext, world, serverPlayer)) {
                            world.dropItemNaturally(position, item);
                        }
                    }
                });
            }
            // sound system
            if (Config.enableSoundSystem() && (!event.isCancelled() || Config.processCancelledBreak())) {
                if (BukkitItemUtils.isDebugStick(itemInHand)) return;
                Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
                Object soundEvent = FastNMS.INSTANCE.field$SoundType$breakSound(soundType);
                Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
                if (this.manager.isBreakSoundMissing(soundId)) {
                    player.playSound(block.getLocation().add(0.5, 0.5, 0.5), soundId.toString(), SoundCategory.BLOCKS, 1f, 0.8f);
                }
            }
        }
    }

    // BlockBreakBlockEvent = liquid + piston
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        Block block = event.getBlock();
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(block.getWorld()), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()));
        if (BlockStateUtils.isVanillaBlock(blockState)) {
            // override vanilla block loots
            this.plugin.vanillaLootManager().getBlockLoot(BlockStateUtils.blockStateToId(blockState)).ifPresent(it -> {
                if (it.override()) {
                    event.getDrops().clear();
                    event.setExpToDrop(0);
                }
                Location location = block.getLocation();
                net.momirealms.craftengine.core.world.World world = BukkitAdaptors.adapt(location.getWorld());
                WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
                ContextHolder.Builder builder = ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position)
                        .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block));
                for (LootTable<?> lootTable : it.lootTables()) {
                    for (Item<?> item : lootTable.getRandomItems(builder.build(), world, null)) {
                        world.dropItemNaturally(position, item);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onStep(GenericGameEvent event) {
        GameEvent gameEvent = event.getEvent();
        // 只处理落地和走路
        if (gameEvent != GameEvent.STEP) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        BlockPos pos = EntityUtils.getOnPos(player);
        Block block = player.getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld()), LocationUtils.toBlockPos(pos));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isPresent()) {
            Location location = player.getLocation();
            ImmutableBlockState state = optionalCustomState.get();
            Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
            state.owner().value().execute(PlayerOptionalContext.of(BukkitAdaptors.adapt(player), ContextHolder.builder()
                    .withParameter(DirectContextParameters.EVENT, cancellable)
                    .withParameter(DirectContextParameters.POSITION, new WorldPosition(BukkitAdaptors.adapt(event.getWorld()), LocationUtils.toVec3d(location)))
                    .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                    .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, state)
            ), EventTrigger.STEP);
            if (cancellable.isCancelled() && !Config.processCancelledStep()) {
                return;
            }
            SoundData soundData = state.settings().sounds().stepSound();
            player.playSound(location, soundData.id().toString(), SoundCategory.BLOCKS, soundData.volume().get(), soundData.pitch().get());
        } else if (Config.enableSoundSystem()) {
            if (event.isCancelled() && !Config.processCancelledStep()) {
                return;
            }
            Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
            Object soundEvent = FastNMS.INSTANCE.field$SoundType$stepSound(soundType);
            Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
            if (this.manager.isStepSoundMissing(soundId)) {
                player.playSound(player.getLocation(), soundId.toString(), SoundCategory.BLOCKS, 0.15f, 1f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        if (!(event.getEntity() instanceof Player player)) return;
        BlockPos pos = EntityUtils.getOnPos(player);
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld()), LocationUtils.toBlockPos(pos));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isPresent()) {
            Location location = player.getLocation();
            ImmutableBlockState state = optionalCustomState.get();
            SoundData soundData = state.settings().sounds().fallSound();
            player.playSound(location, soundData.id().toString(), SoundCategory.BLOCKS, soundData.volume().get(), soundData.pitch().get());
        } else if (Config.enableSoundSystem()) {
            if (event.isCancelled() && !Config.processCancelledStep()) {
                return;
            }
            Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
            Object soundEvent = FastNMS.INSTANCE.field$SoundType$fallSound(soundType);
            Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
            if (this.manager.isStepSoundMissing(soundId)) {
                player.playSound(player.getLocation(), soundId.toString(), SoundCategory.BLOCKS, 0.15f, 1f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        // for vanilla blocks
        if (event.getChangedType() == Material.NOTE_BLOCK) {
            Block block = event.getBlock();
            Block sourceBlock = event.getSourceBlock();
            if (block.getX() == sourceBlock.getX() && block.getX() == sourceBlock.getZ()) {
                World world = block.getWorld();
                Location location = block.getLocation();
                Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(world);
                Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
                Object blockPos = LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, blockPos);
                if (block.getY() > sourceBlock.getY()) {
                    NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, CoreReflections.instance$Direction$UP, blockPos, Config.maxNoteBlockChainUpdate());
                } else {
                    NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, CoreReflections.instance$Direction$DOWN, blockPos, Config.maxNoteBlockChainUpdate());
                }
            }
        }
    }
}
