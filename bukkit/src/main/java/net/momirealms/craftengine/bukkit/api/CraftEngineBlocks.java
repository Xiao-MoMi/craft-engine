package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CraftEngineBlocks {

    private CraftEngineBlocks() {}

    /**
     *
     * Returns an unmodifiable map of all currently loaded custom blocks.
     * The map keys represent unique identifiers, and the values are the corresponding CustomBlock instances.
     *
     * <p><strong>Important:</strong> Do not attempt to access this method during the onEnable phase
     * as it will be empty. Instead, listen for the {@code CraftEngineReloadEvent} and use this method
     * after the event is fired to obtain the complete block list.
     *
     * @return a non-null map containing all loaded custom blocks
     */
    @NotNull
    public static Map<Key, CustomBlock> loadedBlocks() {
        return BukkitBlockManager.instance().loadedBlocks();
    }

    /**
     * Gets a custom block by ID
     *
     * @param id id
     * @return the custom block
     */
    @Nullable
    public static CustomBlock byId(@NotNull Key id) {
        return BukkitBlockManager.instance().blockById(id).orElse(null);
    }

    /**
     * Places a custom block state at a certain location
     *
     * @param location location
     * @param block block state to place
     * @param playSound whether to play place sounds
     * @return success or not
     */
    public static boolean place(@NotNull Location location,
                                @NotNull ImmutableBlockState block,
                                boolean playSound) {
        return place(location, block, UpdateOption.UPDATE_ALL, playSound);
    }

    /**
     * Place a custom block
     *
     * @param location location
     * @param blockId block owner id
     * @param playSound whether to play place sounds
     * @return success or not
     */
    public static boolean place(@NotNull Location location,
                                @NotNull Key blockId,
                                boolean playSound) {
        CustomBlock block = byId(blockId);
        if (block == null) return false;
        return place(location, block.defaultState(), UpdateOption.UPDATE_ALL, playSound);
    }

    /**
     * Place a custom block with given properties
     *
     * @param location location
     * @param blockId block owner id
     * @param properties properties
     * @param playSound whether to play place sounds
     * @return success or not
     */
    public static boolean place(@NotNull Location location,
                                @NotNull Key blockId,
                                @NotNull CompoundTag properties,
                                boolean playSound) {
        CustomBlock block = byId(blockId);
        if (block == null) return false;
        return place(location, block.getBlockState(properties), UpdateOption.UPDATE_ALL, playSound);
    }

    /**
     * Place a custom block with given properties
     *
     * @param location location
     * @param blockId block owner id
     * @param properties properties
     * @param option update options
     * @param playSound whether to play place sounds
     * @return success or not
     */
    public static boolean place(@NotNull Location location,
                                @NotNull Key blockId,
                                @NotNull CompoundTag properties,
                                @NotNull UpdateOption option,
                                boolean playSound) {
        CustomBlock block = byId(blockId);
        if (block == null) return false;
        return place(location, block.getBlockState(properties), option, playSound);
    }

    /**
     * Places a custom block state at a certain location
     *
     * @param location location
     * @param block block state to place
     * @param option update options
     * @param playSound whether to play place sounds
     * @return success or not
     */
    public static boolean place(@NotNull Location location,
                                @NotNull ImmutableBlockState block,
                                @NotNull UpdateOption option,
                                boolean playSound) {
        boolean success;
        Object worldServer = CraftWorldProxy.INSTANCE.getWorld(location.getWorld());
        Object blockPos = BlockPosProxy.INSTANCE.newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Object blockState = block.customBlockState().literalObject();
        Object oldBlockState = BlockGetterProxy.INSTANCE.getBlockState(worldServer, blockPos);
        success = LevelWriterProxy.INSTANCE.setBlock(worldServer, blockPos, blockState, option.flags());
        if (success) {
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.onPlace(blockState, worldServer, blockPos, oldBlockState, false);
            if (playSound) {
                SoundData data = block.settings().sounds().placeSound();
                location.getWorld().playSound(location, data.id().toString(), SoundCategory.BLOCKS, data.volume().get(), data.pitch().get());
            }
        }
        return success;
    }

    /**
     * Removes a block from the world if it's custom
     *
     * @param block block to remove
     * @return success or not
     */
    public static boolean remove(@NotNull Block block) {
        return remove(block, false);
    }

    /**
     * Removes a block from the world if it's custom
     *
     * @param block block to remove
     * @param movedByPiston moved by piston
     * @return success or not
     */
    public static boolean remove(@NotNull Block block,
                                 boolean movedByPiston) {
        if (!isCustomBlock(block)) return false;
        LevelProxy.INSTANCE.removeBlock(CraftWorldProxy.INSTANCE.getWorld(block.getWorld()), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()), movedByPiston);
        return true;
    }

    /**
     * Removes a block from the world if it's custom
     *
     * @param block block to remove
     * @param player player who breaks the block
     * @param dropLoot whether to drop block loots
     * @param movedByPiston moved by piston
     * @param sendLevelEvent whether to send break particles and sounds
     * @return success or not
     */
    public static boolean remove(@NotNull Block block,
                                 @Nullable Player player,
                                 boolean movedByPiston,
                                 boolean dropLoot,
                                 boolean sendLevelEvent) {
        ImmutableBlockState state = getCustomBlockState(block);
        if (state == null || state.isEmpty()) return false;
        World world = BukkitAdaptors.adapt(block.getWorld());
        Location location = block.getLocation();
        WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        if (dropLoot) {
            ContextHolder.Builder builder = new ContextHolder.Builder()
                    .withParameter(DirectContextParameters.POSITION, position);
            BukkitServerPlayer serverPlayer = null;
            if (player != null) {
                serverPlayer = BukkitCraftEngine.instance().adapt(player);
                builder.withOptionalParameter(DirectContextParameters.PLAYER, serverPlayer);
            }
            for (Item<?> item : state.getDrops(builder, world, serverPlayer)) {
                world.dropItemNaturally(position, item);
            }
        }
        if (sendLevelEvent) {
            LevelAccessorProxy.INSTANCE.levelEvent(world.serverWorld(), WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), state.customBlockState().registryId());
        }
        LevelProxy.INSTANCE.removeBlock(world.serverWorld(), LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), movedByPiston);
        return true;
    }

    /**
     * Removes a block from the world if it's custom
     *
     * @param block block to remove
     * @param player player who breaks the block
     * @param dropLoot whether to drop block loots
     * @param isMoving is moving
     * @param playSound whether to play break sounds
     * @param sendParticles whether to send break particles
     * @return success or not
     */
    @Deprecated(forRemoval = true)
    public static boolean remove(@NotNull Block block,
                                 @Nullable Player player,
                                 boolean isMoving,
                                 boolean dropLoot,
                                 boolean playSound,
                                 boolean sendParticles) {
        return remove(block, player, dropLoot, isMoving, playSound || sendParticles);
    }

    /**
     * Checks if a block is custom
     *
     * @param block block
     * @return is custom block or not
     */
    public static boolean isCustomBlock(@NotNull Block block) {
        Object state = BlockGetterProxy.INSTANCE.getBlockState(CraftWorldProxy.INSTANCE.getWorld(block.getWorld()), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()));
        return BlockStateUtils.isCustomBlock(state);
    }

    /**
     * Gets custom block state from a bukkit block
     *
     * @param block block
     * @return custom block state
     */
    @Nullable
    public static ImmutableBlockState getCustomBlockState(@NotNull Block block) {
        Object state = BlockGetterProxy.INSTANCE.getBlockState(CraftWorldProxy.INSTANCE.getWorld(block.getWorld()), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()));
        return BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
    }

    /**
     * Gets custom block state from bukkit block data
     *
     * @param blockData block data
     * @return custom block state
     */
    @Nullable
    public static ImmutableBlockState getCustomBlockState(@NotNull BlockData blockData) {
        Object state = BlockStateUtils.blockDataToBlockState(blockData);
        return BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
    }

    /**
     * Creates bukkit block data from a custom block state
     *
     * @param blockState custom block state
     * @return bukkit block data
     */
    @NotNull
    public static BlockData getBukkitBlockData(@NotNull ImmutableBlockState blockState) {
        return BlockStateUtils.fromBlockData(blockState.customBlockState().literalObject());
    }

    /**
     * Checks if the block state is a vanilla block state
     *
     * @param id state id
     * @return is vanilla block or not
     */
    public static boolean isVanillaBlockState(int id) {
        return BukkitBlockManager.instance().isVanillaBlockState(id);
    }
}
