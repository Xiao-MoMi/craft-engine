package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.IdMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class BlockStateUtils {
    private BlockStateUtils() {}

    public static boolean isTag(BlockData blockData, Key tag) {
        return isTag(blockDataToBlockState(blockData), tag);
    }

    public static boolean isTag(Object blockState, Key tag) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(blockState, BlockTags.getOrCreate(tag));
    }

    public static BlockStateWrapper toBlockStateWrapper(BlockData blockData) {
        Object state = blockDataToBlockState(blockData);
        return toBlockStateWrapper(state);
    }

    public static BlockStateWrapper toBlockStateWrapper(Object blockState) {
        int id = blockStateToId(blockState);
        return BlockRegistryMirror.byId(id);
    }

    public static boolean isCorrectTool(@NotNull ImmutableBlockState state, @Nullable Item<ItemStack> itemInHand) {
        BlockSettings settings = state.settings();
        if (settings.requireCorrectTool()) {
            if (itemInHand == null || itemInHand.isEmpty()) return false;
            return settings.isCorrectTool(itemInHand.id()) ||
                    (settings.respectToolComponent() && FastNMS.INSTANCE.method$ItemStack$isCorrectToolForDrops(itemInHand.getLiteralObject(), state.customBlockState().literalObject()));
        }
        return true;
    }

    public static List<Object> getPossibleBlockStates(Key block) {
        Object blockIns = RegistryUtils.getRegistryValue(MBuiltInRegistries.BLOCK, KeyUtils.toIdentifier(block));
        Object definition = BlockProxy.INSTANCE.getStateDefinition(blockIns);
        return StateDefinitionProxy.INSTANCE.getStates(definition);
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
        Object blockOwner = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(blockState);
        Object identifier = RegistryProxy.INSTANCE.getKey(MBuiltInRegistries.BLOCK, blockOwner);
        return KeyUtils.identifierToKey(identifier);
    }

    public static Object blockDataToBlockState(BlockData blockData) {
        return FastNMS.INSTANCE.method$CraftBlockData$getState(blockData);
    }

    public static Object idToBlockState(int id) {
        return IdMapProxy.INSTANCE.byId(BlockProxy.BLOCK_STATE_REGISTRY, id);
    }

    public static int blockStateToId(Object blockState) {
        return IdMapProxy.INSTANCE.getId$1(BlockProxy.BLOCK_STATE_REGISTRY, blockState);
    }

    public static Object getBlockOwner(Object blockState) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(blockState);
    }

    public static boolean isOcclude(Object state) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isCanOcclude(state);
    }

    public static boolean isReplaceable(Object state) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isReplaceable(state);
    }

    public static boolean isVanillaBlock(Object state) {
        return !(state instanceof DelegatingBlockState);
    }

    public static boolean isCustomBlock(Object state) {
        return state instanceof DelegatingBlockState;
    }

    public static boolean isVanillaBlock(int id) {
        return BukkitBlockManager.instance().isVanillaBlockState(id);
    }

    public static int vanillaBlockStateCount() {
        return BukkitBlockManager.instance().vanillaBlockStateCount();
    }

    public static Optional<ImmutableBlockState> getOptionalCustomBlockState(Object state) {
        if (state instanceof DelegatingBlockState holder) {
            return Optional.ofNullable(holder.blockState());
        } else {
            return Optional.empty();
        }
    }

    public static Object getBlockState(Block block) {
        return FastNMS.INSTANCE.method$BlockGetter$getBlockState(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(block.getWorld()), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()));
    }

    public static boolean isBurnable(Object blockState) {
        return BukkitBlockManager.instance().isBurnable(blockState);
    }
}
