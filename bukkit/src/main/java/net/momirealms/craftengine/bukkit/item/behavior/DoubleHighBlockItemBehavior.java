package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class DoubleHighBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<DoubleHighBlockItemBehavior> FACTORY = new Factory();

    private DoubleHighBlockItemBehavior(Key blockId) {
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

    private static class Factory implements ItemBehaviorFactory<DoubleHighBlockItemBehavior> {
        @Override
        public DoubleHighBlockItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.double_high.missing_block");
            }
            if (id instanceof Map<?, ?> map) {
                addPendingSection(pack, path, node, key, map);
                return new DoubleHighBlockItemBehavior(key);
            } else {
                return new DoubleHighBlockItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
