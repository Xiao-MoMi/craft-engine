package net.momirealms.craftengine.bukkit.block.listener;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.customdata.BlockDebugStickData;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSystemChatPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.AbilitiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

import static net.momirealms.craftengine.core.block.UpdateFlags.UPDATE_CLIENTS;
import static net.momirealms.craftengine.core.block.UpdateFlags.UPDATE_KNOWN_SHAPE;


public final class BlockEventListener implements Listener {
    private static final String DEBUG_STICK_TAG = "craftengine:debug_stick_state";
    private final BukkitCraftEngine plugin;
    private final BukkitBlockManager manager;

    public BlockEventListener(BukkitCraftEngine plugin, BukkitBlockManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!VersionHelper.isOrAbove1_20_5) {
            if (event.getDamager() instanceof Player player) {
                BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                if (serverPlayer == null) return;
                serverPlayer.setClientSideCanBreakBlock(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceBlock(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        // send swing if player is clicking a replaceable block
        if (serverPlayer.shouldResendSwing()) {
            serverPlayer.swingHand(event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
        // send sound if the placed block's sounds are removed
        if (Config.enableSoundSystem()) {
            Block block = event.getBlock();
            Object blockState = BlockStateUtils.getBlockState(block);
            if (BlockStateUtils.isVanillaBlock(blockState)) {
                Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
                Object soundEvent = SoundTypeProxy.INSTANCE.getPlaceSound(soundType);
                Object soundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
                if (this.manager.isPlaceSoundMissing(soundId)) {
                    // 概率出现放置空气
                    if (blockState == BlocksProxy.AIR$defaultState) {
                        return;
                    }
                    // 打火石有逆天羊毛音效
                    Object blockOwner = BlockStateUtils.getBlockOwner(blockState);
                    if (blockOwner == BlocksProxy.FIRE || blockOwner == BlocksProxy.SOUL_FIRE) {
                        return;
                    }
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
            Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
            Object soundEvent = SoundTypeProxy.INSTANCE.getPlaceSound(soundType);
            Object soundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
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
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        serverPlayer.updateLastSuccessBreakTick();
        net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(player.getWorld());
        BlockPos blockPos = LocationUtils.toBlockPos(location);
        WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        BukkitItem itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (!event.isCancelled() && !ItemUtils.isEmpty(itemInHand)) {
            Optional<ItemDefinition> optionalCustomItem = itemInHand.getDefinition();
            if (optionalCustomItem.isPresent()) {
                ItemDefinition itemDefinition = optionalCustomItem.get();
                Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
                itemDefinition.execute(
                        PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(block))
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand)
                        ), EventTrigger.BREAK
                );
                if (cancellable.isCancelled()) {
                    return;
                }
                itemDefinition.behavior().onBreakBlock(world, serverPlayer, blockPos);
            }
        }

        if (BlockStateUtils.isVanillaBlock(stateId)) {
            // sound system
            if (Config.enableSoundSystem() && (!event.isCancelled() || Config.processCancelledBreak())) {
                if (BukkitItemUtils.isDebugStick(itemInHand)) return;
                Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
                Object soundEvent = SoundTypeProxy.INSTANCE.getBreakSound(soundType);
                Object soundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
                if (this.manager.isBreakSoundMissing(soundId)) {
                    // creative mode + invalid item in hand
                    if (serverPlayer.canInstabuild() && !ItemStackUtils.canBreakBlockInCreativeMode(itemInHand)) {
                        return;
                    }
                    Object blockOwner = BlockStateUtils.getBlockOwner(blockState);
                    if (blockOwner == BlocksProxy.FIRE || blockOwner == BlocksProxy.SOUL_FIRE) {
                        return;
                    }
                    player.playSound(block.getLocation().add(0.5, 0.5, 0.5), soundId.toString(), SoundCategory.BLOCKS, 1f, 0.8f);
                }
            }
        } else {
            ImmutableBlockState state = this.manager.getImmutableBlockStateUnsafe(stateId);
            if (!state.isEmpty()) {
                if (!event.isCancelled()) {
                    // double check adventure mode to prevent dupe
                    Object abilities = PlayerProxy.INSTANCE.getAbilities(serverPlayer.minecraftPlayer());
                    if (!AbilitiesProxy.INSTANCE.isMayBuild(abilities) && !serverPlayer.canBreak(blockPos, null)) {
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
                            .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand)
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
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(CraftWorldProxy.INSTANCE.getWorld(player.getWorld()), LocationUtils.toBlockPos(pos));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isPresent()) {
            Location location = player.getLocation();
            ImmutableBlockState state = optionalCustomState.get();
            Cancellable cancellable = Cancellable.of(event::isCancelled, event::setCancelled);
            state.owner().value().execute(PlayerOptionalContext.of(BukkitAdaptor.adapt(player), ContextHolder.builder()
                    .withParameter(DirectContextParameters.EVENT, cancellable)
                    .withParameter(DirectContextParameters.POSITION, new WorldPosition(BukkitAdaptor.adapt(event.getWorld()), LocationUtils.toVec3d(location)))
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
            Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
            Object soundEvent = SoundTypeProxy.INSTANCE.getStepSound(soundType);
            Object soundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
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
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(CraftWorldProxy.INSTANCE.getWorld(player.getWorld()), LocationUtils.toBlockPos(pos));
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
            Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
            Object soundEvent = SoundTypeProxy.INSTANCE.getFallSound(soundType);
            Object soundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
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
                Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(world);
                Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
                Object blockPos = LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                ServerChunkCacheProxy.INSTANCE.blockChanged(chunkSource, blockPos);
                if (block.getY() > sourceBlock.getY()) {
                    NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, DirectionProxy.UP, blockPos, Config.maxNoteBlockChainUpdate());
                } else {
                    NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, DirectionProxy.DOWN, blockPos, Config.maxNoteBlockChainUpdate());
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseDebugStick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = BukkitAdaptor.adapt(bukkitPlayer);
        if (player == null) return;
        BukkitItem itemInHand = player.getItemInHand(event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!BukkitItemUtils.isDebugStick(itemInHand)) return;
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
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(CraftWorldProxy.INSTANCE.getWorld(clickedBlock.getWorld()), LocationUtils.toBlockPos(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()));
        BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
            event.setCancelled(true);
            boolean update = event.getAction() == Action.RIGHT_CLICK_BLOCK;
            BlockDefinition block = customState.owner().value();
            Collection<Property<?>> properties = block.properties();
            if (properties.isEmpty()) {
                Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(
                        ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.empty").arguments(Component.text(block.id().asString()))), true);
                player.sendPacket(systemChatPacket, false);
            } else {
                BlockDebugStickData debugStickData = Optional.ofNullable(itemInHand.getCustomData(BlockDebugStickData.class, DEBUG_STICK_TAG, "block")).orElseGet(BlockDebugStickData::new);
                Property<?> currentProperty = debugStickData.getProperty(block);
                if (update) {
                    ImmutableBlockState nextState = cycleState(customState, currentProperty, player.isSecondaryUseActive());
                    CraftEngineBlocks.place(clickedBlock.getLocation(), nextState, UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE, false);
                    Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(
                            ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.update")
                                    .arguments(
                                            Component.text(currentProperty.name()),
                                            Component.text(getNameHelper(nextState, currentProperty))
                                    )), true);
                    player.sendPacket(systemChatPacket, false);
                } else {
                    currentProperty = getRelative(properties, currentProperty, player.isSecondaryUseActive());
                    debugStickData.setProperty(block, currentProperty);
                    itemInHand.setCustomData(debugStickData, DEBUG_STICK_TAG, "block");
                    Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(
                            ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.select")
                                    .arguments(
                                            Component.text(currentProperty.name()),
                                            Component.text(getNameHelper(customState, currentProperty))
                                    )), true);
                    player.sendPacket(systemChatPacket, false);
                }
            }
        });
    }

    private static <T extends Comparable<T>> ImmutableBlockState cycleState(ImmutableBlockState state, Property<T> property, boolean inverse) {
        return state.with(property, getRelative(property.possibleValues(), state.get(property), inverse));
    }

    private static <T> T getRelative(Iterable<T> elements, @Nullable T current, boolean inverse) {
        return inverse ? MiscUtils.findPreviousInIterable(elements, current) : MiscUtils.findNextInIterable(elements, current);
    }

    private static <T extends Comparable<T>> String getNameHelper(ImmutableBlockState state, Property<T> property) {
        return property.valueName(state.get(property));
    }
}
