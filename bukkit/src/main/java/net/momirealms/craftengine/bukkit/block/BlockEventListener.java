package net.momirealms.craftengine.bukkit.block;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitBlockInWorld;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class BlockEventListener implements Listener {
    private final BukkitCraftEngine plugin;
    private final boolean enableNoteBlockCheck;
    private final BukkitBlockManager manager;

    public BlockEventListener(BukkitCraftEngine plugin, BukkitBlockManager manager, boolean enableNoteBlockCheck) {
        this.plugin = plugin;
        this.manager = manager;
        this.enableNoteBlockCheck = enableNoteBlockCheck;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Object packet = this.manager.cachedUpdateTagsPacket;
        if (packet != null) {
            this.plugin.networkManager().sendPacket(event.getPlayer(), packet);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!VersionHelper.isOrAbove1_20_5()) {
            if (event.getDamager() instanceof Player player) {
                BukkitServerPlayer serverPlayer = plugin.adapt(player);
                serverPlayer.setClientSideCanBreakBlock(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceBlock(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = plugin.adapt(player);
        // send swing if player is clicking a replaceable block
        if (serverPlayer.shouldResendSwing()) {
            player.swingHand(event.getHand());
        }
        // send sound if the placed block's sounds are removed
        if (Config.enableSoundSystem()) {
            Block block = event.getBlock();
            Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
            if (blockState != Reflections.instance$Blocks$AIR$defaultState) {
                Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
                if (this.manager.isBlockSoundRemoved(ownerBlock)) {
                    if (player.getInventory().getItemInMainHand().getType() != Material.DEBUG_STICK) {
                        try {
                            Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                            Object placeSound = Reflections.field$SoundType$placeSound.get(soundType);
                            player.playSound(block.getLocation(), FastNMS.INSTANCE.field$SoundEvent$location(placeSound).toString(), SoundCategory.BLOCKS, 1f, 0.8f);
                        } catch (ReflectiveOperationException e) {
                            this.plugin.logger().warn("Failed to get sound type", e);
                        }
                    }
                    return;
                }
            }
        }
        // resend sound if the clicked block is interactable on client side
        if (serverPlayer.shouldResendSound()) {
            try {
                Block block = event.getBlock();
                Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
                Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
                Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                Object placeSound = Reflections.field$SoundType$placeSound.get(soundType);
                player.playSound(block.getLocation(), FastNMS.INSTANCE.field$SoundEvent$location(placeSound).toString(), SoundCategory.BLOCKS, 1f, 0.8f);
            } catch (ReflectiveOperationException e) {
                this.plugin.logger().warn("Failed to get sound type", e);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerBreak(BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
        int stateId = BlockStateUtils.blockStateToId(blockState);
        Player player = event.getPlayer();
        Location location = block.getLocation();
        BukkitServerPlayer serverPlayer = this.plugin.adapt(player);
        net.momirealms.craftengine.core.world.World world = new BukkitWorld(player.getWorld());
        WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        Item<ItemStack> itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (itemInHand != null) {
            Optional<CustomItem<ItemStack>> optionalCustomItem = itemInHand.getCustomItem();
            if (optionalCustomItem.isPresent()) {
                Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
                optionalCustomItem.get().execute(
                        PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                        ), EventTrigger.BREAK
                );
                if (cancellable.isCancelled()) {
                    return;
                }
            }
        }

        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            ImmutableBlockState state = manager.getImmutableBlockStateUnsafe(stateId);
            if (!state.isEmpty()) {
                // double check adventure mode to prevent dupe
                if (!FastNMS.INSTANCE.mayBuild(serverPlayer.serverPlayer()) && !serverPlayer.canBreak(LocationUtils.toBlockPos(location), null)) {
                    return;
                }

                // trigger api event
                CustomBlockBreakEvent customBreakEvent = new CustomBlockBreakEvent(serverPlayer, location, block, state);
                boolean isCancelled = EventUtils.fireAndCheckCancel(customBreakEvent);
                if (isCancelled) {
                    event.setCancelled(true);
                    return;
                }

                // execute functions
                Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                        .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                        .withParameter(DirectContextParameters.BLOCK_STATE, state)
                        .withParameter(DirectContextParameters.EVENT, cancellable)
                        .withParameter(DirectContextParameters.POSITION, position)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                );
                state.owner().value().execute(context, EventTrigger.BREAK);
                if (cancellable.isCancelled()) {
                    return;
                }

                // handle waterlogged blocks
                @SuppressWarnings("unchecked")
                Property<Boolean> waterloggedProperty = (Property<Boolean>) state.owner().value().getProperty("waterlogged");
                if (waterloggedProperty != null) {
                    boolean waterlogged = state.get(waterloggedProperty);
                    if (waterlogged) {
                        location.getWorld().setBlockData(location, Material.WATER.createBlockData());
                    }
                }

                // play sound
                world.playBlockSound(position, state.sounds().breakSound());
                if (player.getGameMode() == GameMode.CREATIVE || !customBreakEvent.dropItems()) {
                    return;
                }

                // do not drop if it's not the correct tool
                if (!BlockStateUtils.isCorrectTool(state, itemInHand)) {
                    return;
                }

                // drop items
                ContextHolder.Builder lootContext = ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position)
                        .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                        .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand);
                for (Item<Object> item : state.getDrops(lootContext, world, serverPlayer)) {
                    world.dropItemNaturally(position, item);
                }
            }
        } else {
            // override vanilla block loots
            if (player.getGameMode() != GameMode.CREATIVE) {
                this.plugin.vanillaLootManager().getBlockLoot(stateId).ifPresent(it -> {
                    if (it.override()) {
                        event.setDropItems(false);
                        event.setExpToDrop(0);
                    }
                    ContextHolder lootContext = ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand).build();
                    for (LootTable<?> lootTable : it.lootTables()) {
                        for (Item<?> item : lootTable.getRandomItems(lootContext, world, serverPlayer)) {
                            world.dropItemNaturally(position, item);
                        }
                    }
                });
            }

            // sound system
            if (Config.enableSoundSystem()) {
                Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
                if (this.manager.isBlockSoundRemoved(ownerBlock)) {
                    try {
                        Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                        Object breakSound = Reflections.field$SoundType$breakSound.get(soundType);
                        block.getWorld().playSound(block.getLocation(), FastNMS.INSTANCE.field$SoundEvent$location(breakSound).toString(), SoundCategory.BLOCKS, 1f, 0.8f);
                    } catch (ReflectiveOperationException e) {
                        this.plugin.logger().warn("Failed to get sound type", e);
                    }
                }
            }
        }
    }

    // BlockBreakBlockEvent = liquid + piston
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        Block block = event.getBlock();
        Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
        int stateId = BlockStateUtils.blockStateToId(blockState);
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            // custom blocks
            ImmutableBlockState immutableBlockState = this.manager.getImmutableBlockStateUnsafe(stateId);
            if (!immutableBlockState.isEmpty()) {
                Location location = block.getLocation();
                net.momirealms.craftengine.core.world.World world = new BukkitWorld(block.getWorld());
                WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
                for (Item<?> item : immutableBlockState.getDrops(ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position)
                        .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block)), world, null)) {
                    world.dropItemNaturally(position, item);
                }
            }
        } else {
            // override vanilla block loots
            this.plugin.vanillaLootManager().getBlockLoot(stateId).ifPresent(it -> {
                if (it.override()) {
                    event.getDrops().clear();
                    event.setExpToDrop(0);
                }
                Location location = block.getLocation();
                net.momirealms.craftengine.core.world.World world = new BukkitWorld(location.getWorld());
                WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
                ContextHolder.Builder builder = ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position)
                        .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block));
                for (LootTable<?> lootTable : it.lootTables()) {
                    for (Item<?> item : lootTable.getRandomItems(builder.build(), world, null)) {
                        world.dropItemNaturally(position, item);
                    }
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onStep(GenericGameEvent event) {
        if (event.getEvent() != GameEvent.STEP) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        BlockPos pos = EntityUtils.getOnPos(player);
        Block block = player.getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
        Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
        int stateId = BlockStateUtils.blockStateToId(blockState);
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            Location location = player.getLocation();
            ImmutableBlockState state = manager.getImmutableBlockStateUnsafe(stateId);
            Cancellable cancellable = Cancellable.dummy();
            state.owner().value().execute(PlayerOptionalContext.of(this.plugin.adapt(player), ContextHolder.builder()
                    .withParameter(DirectContextParameters.EVENT, cancellable)
                    .withParameter(DirectContextParameters.POSITION, new WorldPosition(new BukkitWorld(event.getWorld()), LocationUtils.toVec3d(location)))
                    .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block))
                    .withParameter(DirectContextParameters.BLOCK_STATE, state)
            ), EventTrigger.STEP);
            if (cancellable.isCancelled()) {
                event.setCancelled(true);
                return;
            }
            player.playSound(location, state.sounds().stepSound().id().toString(), SoundCategory.BLOCKS, state.sounds().stepSound().volume(), state.sounds().stepSound().pitch());
        } else if (Config.enableSoundSystem()) {
            Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
            if (manager.isBlockSoundRemoved(ownerBlock)) {
                try {
                    Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                    Object stepSound = Reflections.field$SoundType$stepSound.get(soundType);
                    player.playSound(player.getLocation(), FastNMS.INSTANCE.field$SoundEvent$location(stepSound).toString(), SoundCategory.BLOCKS, 0.15f, 1f);
                } catch (ReflectiveOperationException e) {
                    plugin.logger().warn("Failed to get sound type", e);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (VersionHelper.isOrAbove1_21()) {
            if (!ExplosionUtils.isDroppingItems(event)) return;
        }
        handleExplodeEvent(event.blockList(), new BukkitWorld(event.getEntity().getWorld()), event.getYield());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (VersionHelper.isOrAbove1_21()) {
            if (!ExplosionUtils.isDroppingItems(event)) return;
        }
        handleExplodeEvent(event.blockList(), new BukkitWorld(event.getBlock().getWorld()), event.getYield());
    }

    private void handleExplodeEvent(List<org.bukkit.block.Block> blocks, net.momirealms.craftengine.core.world.World world, float yield) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block block = blocks.get(i);
            Location location = block.getLocation();
            BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            ImmutableBlockState state = manager.getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
            if (state != null && !state.isEmpty()) {
                WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(blockPos));
                ContextHolder.Builder builder = ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position)
                        .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(block));
                if (yield < 1f) {
                    builder.withParameter(DirectContextParameters.EXPLOSION_RADIUS, 1.0f / yield);
                }
                for (Item<Object> item : state.getDrops(builder, world, null)) {
                    world.dropItemNaturally(position, item);
                }
                world.playBlockSound(position, state.sounds().breakSound());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!this.enableNoteBlockCheck) return;
        // for vanilla blocks
        if (event.getChangedType() == Material.NOTE_BLOCK) {
            Block block = event.getBlock();
            World world = block.getWorld();
            Location location = block.getLocation();
            Block sourceBlock = event.getSourceBlock();
            BlockFace direction = sourceBlock.getFace(block);
            if (direction == BlockFace.UP || direction == BlockFace.DOWN) {
                Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(world);
                Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
                Object blockPos = LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, blockPos);
                if (direction == BlockFace.UP) {
                    NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, Reflections.instance$Direction$UP, blockPos, 0);
                } else {
                    NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, Reflections.instance$Direction$DOWN, blockPos, 0);
                }
            }
        }
    }
}
