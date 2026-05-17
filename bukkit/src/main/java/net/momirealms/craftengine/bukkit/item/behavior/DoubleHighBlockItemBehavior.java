package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.block.UpdateFlags.*;

public final class DoubleHighBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<DoubleHighBlockItemBehavior> FACTORY = new Factory();

    private DoubleHighBlockItemBehavior(Key blockId) {
        super(blockId);
    }

    @Override
    protected boolean placeBlock(Location location, ImmutableBlockState blockState, List<BlockState> revertState) {
        Object level = CraftWorldProxy.INSTANCE.getWorld(location.getWorld());
        Object blockPos = BlockPosProxy.INSTANCE.newInstance(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
        Object fluidData = BlockGetterProxy.INSTANCE.getFluidState(level, blockPos);
        Object stateToPlace = fluidData == FluidsProxy.WATER$defaultState ? FluidsProxy.WATER$defaultState : BlocksProxy.AIR$defaultState;
        revertState.add(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ()).getState());
        LevelWriterProxy.INSTANCE.setBlock(level, blockPos, stateToPlace, UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_IMMEDIATE | UPDATE_SUPPRESS_DROPS);
        return super.placeBlock(location, blockState, revertState);
    }

    private static class Factory implements ItemBehaviorFactory<DoubleHighBlockItemBehavior> {
        @Override
        public DoubleHighBlockItemBehavior create(Pack pack, Path path, Key key, ConfigSection section) {
            ConfigValue blockValue = section.getNonNullValue("block", ConfigConstants.ARGUMENT_SECTION);
            if (blockValue.is(Map.class)) {
                BukkitBlockManager.instance().blockParser().addPendingConfigSection(new PendingConfigSection(pack, path, key, blockValue.getAsSection()));
                return new DoubleHighBlockItemBehavior(key);
            } else {
                return new DoubleHighBlockItemBehavior(blockValue.getAsIdentifier());
            }
        }
    }
}
