package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.PushReaction;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

// TODO interact waterlogged blocks
public class BlockEventListener implements Listener {
    private final BukkitCraftEngine plugin;
    private final boolean enableNoteBlockCheck;
    private final BukkitBlockManager manager;

    public BlockEventListener(BukkitCraftEngine plugin, BukkitBlockManager manager, boolean enableNoteBlockCheck) {
        this.plugin = plugin;
        this.manager = manager;
        this.enableNoteBlockCheck = enableNoteBlockCheck;
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
        if (ConfigManager.enableSoundSystem()) {
            Block block = event.getBlock();
            Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
            Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
            if (this.manager.isBlockSoundRemoved(ownerBlock)) {
                if (player.getInventory().getItemInMainHand().getType() != Material.DEBUG_STICK) {
                    try {
                        Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                        Object placeSound = Reflections.field$SoundType$placeSound.get(soundType);
                        player.playSound(block.getLocation(), Reflections.field$SoundEvent$location.get(placeSound).toString(), SoundCategory.BLOCKS, 1f, 1f);
                    } catch (ReflectiveOperationException e) {
                        this.plugin.logger().warn("Failed to get sound type", e);
                    }
                }
                return;
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
                player.playSound(block.getLocation(), Reflections.field$SoundEvent$location.get(placeSound).toString(), SoundCategory.BLOCKS, 1f, 1f);
            } catch (ReflectiveOperationException e) {
                this.plugin.logger().warn("Failed to get sound type", e);
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerBreak(BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
        int stateId = BlockStateUtils.blockStateToId(blockState);
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            ImmutableBlockState state = manager.getImmutableBlockStateUnsafe(stateId);
            if (!state.isEmpty()) {
                Location location = block.getLocation();

                // trigger event
                CustomBlockBreakEvent customBreakEvent = new CustomBlockBreakEvent(event.getPlayer(), location, state);
                boolean isCancelled = EventUtils.fireAndCheckCancel(customBreakEvent);
                if (isCancelled) {
                    event.setCancelled(true);
                    return;
                }

                net.momirealms.craftengine.core.world.World world = new BukkitWorld(location.getWorld());
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
                Vec3d vec3d = new Vec3d(location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
                world.playBlockSound(vec3d, state.sounds().breakSound(), 1f, 1f);

                Player player = event.getPlayer();
                if (player.getGameMode() == GameMode.CREATIVE) {
                    return;
                }

                BukkitServerPlayer serverPlayer = plugin.adapt(player);
                Item<ItemStack> itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                Key itemId = Optional.ofNullable(itemInHand).map(Item::id).orElse(ItemKeys.AIR);
                // do not drop if it's not the correct tool
                if (!state.settings().isCorrectTool(itemId)) {
                    return;
                }
                // drop items
                ContextHolder.Builder builder = ContextHolder.builder();
                builder.withParameter(LootParameters.WORLD, world);
                builder.withParameter(LootParameters.LOCATION, vec3d);
                builder.withParameter(LootParameters.PLAYER, plugin.adapt(player));
                builder.withParameter(LootParameters.TOOL, itemInHand);
                for (Item<Object> item : state.getDrops(builder, world)) {
                    world.dropItemNaturally(vec3d, item);
                }
            }
        } else if (ConfigManager.enableSoundSystem()) {
            Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
            if (manager.isBlockSoundRemoved(ownerBlock)) {
                try {
                    Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                    Object breakSound = Reflections.field$SoundType$breakSound.get(soundType);
                    block.getWorld().playSound(block.getLocation(), Reflections.field$SoundEvent$location.get(breakSound).toString(), SoundCategory.BLOCKS, 1f, 1f);
                } catch (ReflectiveOperationException e) {
                    plugin.logger().warn("Failed to get sound type", e);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStep(GenericGameEvent event) {
        if (event.getEvent() != GameEvent.STEP) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        BlockPos pos = EntityUtils.getOnPos(player);
        Location playerLocation = player.getLocation();
        BlockData blockData = player.getWorld().getBlockData(pos.x(), pos.y(), pos.z());
        Object blockState = BlockStateUtils.blockDataToBlockState(blockData);
        int stateId = BlockStateUtils.blockStateToId(blockState);
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            ImmutableBlockState state = manager.getImmutableBlockStateUnsafe(stateId);
            player.playSound(playerLocation, state.sounds().stepSound().toString(), SoundCategory.BLOCKS, 0.15f, 1f);
        } else if (ConfigManager.enableSoundSystem()) {
            Object ownerBlock = BlockStateUtils.getBlockOwner(blockState);
            if (manager.isBlockSoundRemoved(ownerBlock)) {
                try {
                    Object soundType = Reflections.field$BlockBehaviour$soundType.get(ownerBlock);
                    Object stepSound = Reflections.field$SoundType$stepSound.get(soundType);
                    player.playSound(playerLocation, Reflections.field$SoundEvent$location.get(stepSound).toString(), SoundCategory.BLOCKS, 0.15f, 1f);
                } catch (ReflectiveOperationException e) {
                    plugin.logger().warn("Failed to get sound type", e);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePistonEvent(event.getDirection(), event.getBlocks(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePistonEvent(event.getDirection(), event.getBlocks(), event.getBlock());
    }

    private void handlePistonEvent(BlockFace face, List<Block> blocksList, Block piston) {
        int blocks = blocksList.size();
        net.momirealms.craftengine.core.world.World world = new BukkitWorld(piston.getWorld());
        for (int i = blocks - 1; i >= 0; --i) {
            Location oldLocation = blocksList.get(i).getLocation();
            BlockPos oldPos = new BlockPos(oldLocation.getBlockX(), oldLocation.getBlockY(), oldLocation.getBlockZ());
            Block block = blocksList.get(i);
            ImmutableBlockState blockState = manager.getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
            if (blockState != null && blockState.pushReaction() == PushReaction.DESTROY) {
                // break actions
                ContextHolder.Builder builder = ContextHolder.builder();
                Vec3d vec3d = Vec3d.atCenterOf(oldPos);
                builder.withParameter(LootParameters.LOCATION, vec3d);
                builder.withParameter(LootParameters.WORLD, world);
                for (Item<Object> item : blockState.getDrops(builder, world)) {
                    world.dropItemNaturally(vec3d, item);
                }
                world.playBlockSound(vec3d, blockState.sounds().breakSound(),1f, 1f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplodeEvent(event.blockList(), new BukkitWorld(event.getEntity().getWorld()), event.getYield());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplodeEvent(event.blockList(), new BukkitWorld(event.getBlock().getWorld()), event.getYield());
    }

    private void handleExplodeEvent(List<org.bukkit.block.Block> blocks, net.momirealms.craftengine.core.world.World world, float yield) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block block = blocks.get(i);
            Location location = block.getLocation();
            BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            ImmutableBlockState state = manager.getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
            if (state != null && !state.isEmpty()) {
                ContextHolder.Builder builder = ContextHolder.builder();
                Vec3d vec3d = Vec3d.atCenterOf(blockPos);
                builder.withParameter(LootParameters.LOCATION, vec3d);
                builder.withParameter(LootParameters.WORLD, world);
                if (yield < 1f) {
                    builder.withParameter(LootParameters.EXPLOSION_RADIUS, 1.0f / yield);
                }
                for (Item<Object> item : state.getDrops(builder, world)) {
                    world.dropItemNaturally(vec3d, item);
                }
                world.playBlockSound(vec3d, state.sounds().breakSound(), 1f, 1f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!this.enableNoteBlockCheck) return;
        Block block = event.getBlock();
        // for vanilla blocks
        if (block.getBlockData() instanceof NoteBlock) {
            try {
                World world = block.getWorld();
                Location location = block.getLocation();
                Block sourceBlock = event.getSourceBlock();
                BlockFace direction = sourceBlock.getFace(block);
                if (direction == BlockFace.UP || direction == BlockFace.DOWN) {
                    Object serverLevel = Reflections.field$CraftWorld$ServerLevel.get(world);
                    Object chunkSource = Reflections.field$ServerLevel$chunkSource.get(serverLevel);
                    Object blockPos = Reflections.constructor$BlockPos.newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    Reflections.method$ServerChunkCache$blockChanged.invoke(chunkSource, blockPos);
                    if (direction == BlockFace.UP) {
                        NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, Reflections.instance$Direction$UP, blockPos, 0);
                    } else {
                        NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, Reflections.instance$Direction$DOWN, blockPos, 0);
                    }
                }
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to sync note block states", e);
            }
        }
    }
}
