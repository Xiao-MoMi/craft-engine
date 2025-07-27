package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DoubleHighBlockItemBehavior extends BlockItemBehavior {
    public static final Factory FACTORY = new Factory();

    public DoubleHighBlockItemBehavior(Key blockId) {
        super(blockId);
    }

    @Override
    protected boolean placeBlock(Location location, ImmutableBlockState blockState, List<BlockState> revertState) {
        Object level = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(location.getWorld());
        Object blockPos = FastNMS.INSTANCE.constructor$BlockPos(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
        UpdateOption option = UpdateOption.builder().updateNeighbors().updateClients().updateImmediate().updateKnownShape().build();
        Object fluidData = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, blockPos);
        Object stateToPlace = fluidData == MFluids.WATER$defaultState ? MBlocks.WATER$defaultState : MBlocks.AIR$defaultState;
        revertState.add(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ()).getState());
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, stateToPlace, option.flags());
        return super.placeBlock(location, blockState, revertState);
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Pack pack, Path path, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.double_high.missing_block", new IllegalArgumentException("Missing required parameter 'block' for double_high_block_item behavior"));
            }
            if (id instanceof Map<?, ?> map) {
                if (map.containsKey(key.toString())) {
                    // 防呆
                    BukkitBlockManager.instance().parser().parseSection(pack, path, key, MiscUtils.castToMap(map.get(key.toString()), false));
                } else {
                    BukkitBlockManager.instance().parser().parseSection(pack, path, key, MiscUtils.castToMap(map, false));
                }
                return new DoubleHighBlockItemBehavior(key);
            } else {
                return new DoubleHighBlockItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
