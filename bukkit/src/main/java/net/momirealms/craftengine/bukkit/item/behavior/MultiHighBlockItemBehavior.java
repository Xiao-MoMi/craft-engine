package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.behavior.MultiHighBlockBehavior;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.data.CraftBlockDataProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.block.UpdateFlags.*;

public final class MultiHighBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<MultiHighBlockItemBehavior> FACTORY = new Factory();

    private MultiHighBlockItemBehavior(Key blockId) {
        super(blockId);
    }

    @SuppressWarnings({"UnstableApiUsage", "DuplicatedCode"})
    @Override
    protected boolean canPlace(BlockPlaceContext context, ImmutableBlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        MultiHighBlockBehavior behavior = state.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return false;
        }
        IntegerProperty property = behavior.property;
        Player cePlayer = context.getPlayer();
        Object player = cePlayer != null ? cePlayer.serverPlayer() : null;
        Object blockState = state.customBlockState().literalObject();
        for (int i = property.min + 1; i <= property.max; i++) {
            Object blockPos = LocationUtils.toBlockPos(context.getClickedPos().relative(Direction.UP, i));
            Object voxelShape;
            if (VersionHelper.isOrAbove1_21_6()) {
                voxelShape = CollisionContextProxy.INSTANCE.placementContext(player);
            } else if (player != null) {
                voxelShape = CollisionContextProxy.INSTANCE.of(player);
            } else {
                voxelShape = CollisionContextProxy.INSTANCE.empty();
            }
            Object world = CraftWorldProxy.INSTANCE.getWorld((World) context.getLevel().platformWorld());
            boolean defaultReturn = ServerLevelProxy.INSTANCE.checkEntityCollision(world, blockState, player, voxelShape, blockPos, true); // paper only
            Block block = CraftBlockProxy.INSTANCE.at(world, blockPos);
            BlockData blockData = CraftBlockDataProxy.INSTANCE.fromData(blockState);
            BlockCanBuildEvent canBuildEvent = new BlockCanBuildEvent(
                    block, cePlayer != null ? (org.bukkit.entity.Player) cePlayer.platformPlayer() : null, blockData, defaultReturn,
                    context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
            );
            Bukkit.getPluginManager().callEvent(canBuildEvent);
            if (!canBuildEvent.isBuildable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean placeBlock(Location location, ImmutableBlockState blockState, List<BlockState> revertState) {
        MultiHighBlockBehavior behavior = blockState.behavior().getAs(MultiHighBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return false;
        }
        IntegerProperty property = behavior.property;
        for (int i = property.min + 1; i <= property.max; i++) {
            Object level = CraftWorldProxy.INSTANCE.getWorld(location.getWorld());
            Object blockPos = BlockPosProxy.INSTANCE.newInstance(location.getBlockX(), location.getBlockY() + i, location.getBlockZ());
            Object fluidData = BlockGetterProxy.INSTANCE.getFluidState(level, blockPos);
            Object stateToPlace = fluidData == FluidsProxy.WATER$defaultState ? BlocksProxy.WATER$defaultState : BlocksProxy.AIR$defaultState;
            revertState.add(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + i, location.getBlockZ()).getState());
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, stateToPlace, UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_IMMEDIATE | UPDATE_SUPPRESS_DROPS);
        }
        return super.placeBlock(location, blockState, revertState);
    }

    private static class Factory implements ItemBehaviorFactory<MultiHighBlockItemBehavior> {
        @Override
        public MultiHighBlockItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.multi_high.missing_block");
            }
            if (id instanceof Map<?, ?> map) {
                addPendingSection(pack, path, node, key, map);
                return new MultiHighBlockItemBehavior(key);
            } else {
                return new MultiHighBlockItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
