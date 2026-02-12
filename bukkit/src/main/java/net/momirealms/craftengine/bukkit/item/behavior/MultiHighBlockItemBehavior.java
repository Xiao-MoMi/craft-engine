package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.behavior.MultiHighBlockBehavior;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
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
            try {
                Object voxelShape;
                if (VersionHelper.isOrAbove1_21_6()) {
                    voxelShape = CoreReflections.method$CollisionContext$placementContext.invoke(null, player);
                } else if (player != null) {
                    voxelShape = CoreReflections.method$CollisionContext$of.invoke(null, player);
                } else {
                    voxelShape = CoreReflections.instance$CollisionContext$empty;
                }
                Object world = FastNMS.INSTANCE.field$CraftWorld$ServerLevel((World) context.getLevel().platformWorld());
                boolean defaultReturn = (boolean) CoreReflections.method$ServerLevel$checkEntityCollision.invoke(world, blockState, player, voxelShape, blockPos, true); // paper only
                Block block = FastNMS.INSTANCE.method$CraftBlock$at(world, blockPos);
                BlockData blockData = FastNMS.INSTANCE.method$CraftBlockData$fromData(blockState);
                BlockCanBuildEvent canBuildEvent = new BlockCanBuildEvent(
                        block, cePlayer != null ? (org.bukkit.entity.Player) cePlayer.platformPlayer() : null, blockData, defaultReturn,
                        context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
                );
                Bukkit.getPluginManager().callEvent(canBuildEvent);
                if (!canBuildEvent.isBuildable()) {
                    return false;
                }
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to check canPlace", e);
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
            Object level = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(location.getWorld());
            Object blockPos = FastNMS.INSTANCE.constructor$BlockPos(location.getBlockX(), location.getBlockY() + i, location.getBlockZ());
            UpdateOption option = UpdateOption.builder().updateNeighbors().updateClients().updateImmediate().updateKnownShape().build();
            Object fluidData = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, blockPos);
            Object stateToPlace = fluidData == MFluids.WATER$defaultState ? MBlocks.WATER$defaultState : MBlocks.AIR$defaultState;
            revertState.add(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + i, location.getBlockZ()).getState());
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, stateToPlace, option.flags());
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
