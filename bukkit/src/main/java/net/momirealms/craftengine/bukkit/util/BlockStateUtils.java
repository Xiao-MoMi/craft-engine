package net.momirealms.craftengine.bukkit.util;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.core.block.BlockSettings;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public class BlockStateUtils {
    public static final IdentityHashMap<Object, Object> CLIENT_SIDE_NOTE_BLOCKS = new IdentityHashMap<>();
    private static int vanillaStateSize;
    private static boolean hasInit;
    public static Map<Object, Integer> IGNITE_ODDS;

    @SuppressWarnings("unchecked")
    public static void init(int size) {
        if (hasInit) {
            throw new IllegalStateException("BlockStateUtils has already been initialized");
        }
        vanillaStateSize = size;
        try {
            IGNITE_ODDS = (Map<Object, Integer>) CoreReflections.field$FireBlock$igniteOdds.get(MBlocks.FIRE);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize instance$FireBlock$igniteOdds", e);
        }
        hasInit = true;
    }

    public static BlockStateWrapper toPackedBlockState(BlockData blockData) {
        Object state = blockDataToBlockState(blockData);
        int id = blockStateToId(state);
        return BlockStateWrapper.create(state, id, isVanillaBlock(id));
    }

    public static boolean isCorrectTool(@NotNull ImmutableBlockState state, @Nullable Item<ItemStack> itemInHand) {
        BlockSettings settings = state.settings();
        if (settings.requireCorrectTool()) {
            if (itemInHand == null)
                return false;
            if (!settings.isCorrectTool(itemInHand.id()) &&
                    (!settings.respectToolComponent()
                            || !FastNMS.INSTANCE.method$ItemStack$isCorrectToolForDrops(itemInHand.getLiteralObject(),
                                    state.customBlockState().handle()))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getAllVanillaBlockStates(Key block) {
        try {
            Object blockIns = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK,
                    KeyUtils.toResourceLocation(block));
            Object definition = CoreReflections.field$Block$StateDefinition.get(blockIns);
            return (List<Object>) CoreReflections.field$StateDefinition$states.get(definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get all block states for " + block, e);
        }
    }

    public static BlockData fromBlockData(Object blockState) {
        return FastNMS.INSTANCE.method$CraftBlockData$fromData(blockState);
    }

    public static int blockDataToId(BlockData blockData) {
        return blockStateToId(blockDataToBlockState(blockData));
    }

    public static Key getBlockOwnerIdFromData(BlockData block) {
        return getBlockOwnerIdFromState(blockDataToBlockState(block));
    }

    public static Key getBlockOwnerIdFromState(Object blockState) {
        Object blockOwner = FastNMS.INSTANCE.method$BlockState$getBlock(blockState);
        Object resourceLocation = FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.BLOCK, blockOwner);
        return KeyUtils.resourceLocationToKey(resourceLocation);
    }

    public static Object blockDataToBlockState(BlockData blockData) {
        return FastNMS.INSTANCE.method$CraftBlockData$getState(blockData);
    }

    public static Object idToBlockState(int id) {
        return FastNMS.INSTANCE.method$IdMapper$byId(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY, id);
    }

    public static int blockStateToId(Object blockState) {
        return FastNMS.INSTANCE.method$IdMapper$getId(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY, blockState);
    }

    public static Object getBlockOwner(Object blockState) {
        return FastNMS.INSTANCE.method$BlockState$getBlock(blockState);
    }

    public static boolean isOcclude(Object state) {
        return FastNMS.INSTANCE.method$BlockStateBase$canOcclude(state);
    }

    public static boolean isReplaceable(Object state) {
        return FastNMS.INSTANCE.method$BlockStateBase$isReplaceable(state);
    }

    public static boolean isClientSideNoteBlock(Object state) {
        return CLIENT_SIDE_NOTE_BLOCKS.containsKey(state);
    }

    public static boolean isVanillaBlock(Object state) {
        return !(state instanceof DelegatingBlockState);
    }

    public static boolean isCustomBlock(Object state) {
        return state instanceof DelegatingBlockState;
    }

    public static boolean isVanillaBlock(int id) {
        return id >= 0 && id < vanillaStateSize;
    }

    public static int vanillaStateSize() {
        return vanillaStateSize;
    }

    public static Optional<ImmutableBlockState> getOptionalCustomBlockState(Object state) {
        if (state instanceof DelegatingBlockState holder) {
            return Optional.ofNullable(holder.blockState());
        } else {
            return Optional.empty();
        }
    }

    public static boolean isBurnable(Object state) {
        Object blockOwner = getBlockOwner(state);
        return IGNITE_ODDS.getOrDefault(blockOwner, 0) > 0;
    }

    /**
     * Utility method to remove a BlockStateHitBox placed block and handle container
     * drops
     * 
     * @param placedPosition     The position where the block was placed
     * @param originalBlockState The original block state to restore (can be null
     *                           for air)
     * @param dropContainer      Whether to drop container contents before removal
     * @param actuallyPlaced     Whether we actually placed the block (vs reused
     *                           existing)
     */
    public static void removeBlockStateHitBoxBlock(
            net.momirealms.craftengine.core.world.WorldPosition placedPosition,
            net.momirealms.craftengine.core.block.BlockStateWrapper originalBlockState,
            boolean dropContainer,
            boolean actuallyPlaced) {

        if (placedPosition == null)
            return;

        // Only restore the block if we actually placed it
        if (!actuallyPlaced) {
            net.momirealms.craftengine.core.plugin.CraftEngine.instance().logger()
                    .info("Skipping block removal for reused BlockStateHitBox at position: " + placedPosition);
            return;
        }

        net.momirealms.craftengine.core.world.World world = placedPosition.world();
        int x = (int) placedPosition.x();
        int y = (int) placedPosition.y();
        int z = (int) placedPosition.z();

        // Drop container contents if the flag is enabled
        if (dropContainer) {
            dropContainerContents(world, x, y, z);
        }

        // Restore the original block state with minimal server impact
        if (originalBlockState != null) {
            int flags = net.momirealms.craftengine.core.block.UpdateOption.Flags.UPDATE_CLIENTS |
                    net.momirealms.craftengine.core.block.UpdateOption.Flags.UPDATE_SUPPRESS_DROPS;
            world.setBlockAt(x, y, z, originalBlockState, flags);
        } else {
            // Fallback to air if no original state was stored
            net.momirealms.craftengine.core.block.BlockStateWrapper airState = net.momirealms.craftengine.core.plugin.CraftEngine
                    .instance().blockManager().createPackedBlockState("minecraft:air");
            if (airState != null) {
                int flags = net.momirealms.craftengine.core.block.UpdateOption.Flags.UPDATE_CLIENTS |
                        net.momirealms.craftengine.core.block.UpdateOption.Flags.UPDATE_SUPPRESS_DROPS;
                world.setBlockAt(x, y, z, airState, flags);
            }
        }
    }

    /**
     * Drops the contents of a container block
     */
    private static void dropContainerContents(net.momirealms.craftengine.core.world.World world, int x, int y, int z) {
        try {
            // Get the bukkit world and block
            org.bukkit.World bukkitWorld = (org.bukkit.World) world.platformWorld();
            if (bukkitWorld == null)
                return;

            org.bukkit.block.Block block = bukkitWorld.getBlockAt(x, y, z);
            if (block.getState() instanceof org.bukkit.inventory.InventoryHolder inventoryHolder) {
                org.bukkit.inventory.Inventory inventory = inventoryHolder.getInventory();
                org.bukkit.Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

                // Drop all items in the inventory
                for (org.bukkit.inventory.ItemStack itemStack : inventory.getContents()) {
                    if (itemStack != null && itemStack.getType() != org.bukkit.Material.AIR) {
                        bukkitWorld.dropItemNaturally(dropLocation, itemStack);
                    }
                }

                // Clear the inventory
                inventory.clear();
            }
        } catch (Exception e) {
            net.momirealms.craftengine.core.plugin.CraftEngine.instance().logger()
                    .warn("Failed to drop container contents for BlockStateHitBox", e);
        }
    }
}
