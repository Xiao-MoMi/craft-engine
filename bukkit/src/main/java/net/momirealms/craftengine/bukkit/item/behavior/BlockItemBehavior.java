package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

public class BlockItemBehavior extends ItemBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key blockId;

    public BlockItemBehavior(Key blockId) {
        this.blockId = blockId;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        Optional<CustomBlock> optionalBlock = BukkitBlockManager.instance().getBlock(this.blockId);
        if (optionalBlock.isEmpty()) {
            CraftEngine.instance().logger().warn("Failed to place unknown block " + this.blockId);
            return InteractionResult.FAIL;
        }
        CustomBlock block = optionalBlock.get();
        BlockPlaceContext placeContext = updatePlacementContext(context);
        if (placeContext == null) {
            return InteractionResult.FAIL;
        }
        ImmutableBlockState blockStateToPlace = getPlacementState(placeContext, block);
        if (blockStateToPlace == null) {
            return InteractionResult.FAIL;
        }
        Player player = placeContext.getPlayer();
        int gameTicks = player.gameTicks();
        if (!player.updateLastSuccessfulInteractionTick(gameTicks)) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = placeContext.getClickedPos();
        World world = (World) placeContext.getLevel().getHandle();
        Location placeLocation = new Location(world, pos.x(), pos.y(), pos.z());

        // trigger event
        CustomBlockAttemptPlaceEvent attemptPlaceEvent = new CustomBlockAttemptPlaceEvent((org.bukkit.entity.Player) player.platformPlayer(), placeLocation.clone(), blockStateToPlace,
                DirectionUtils.toBlockFace(context.getClickedFace()), world.getBlockAt(context.getClickedPos().x(), context.getClickedPos().y(), context.getClickedPos().z()), context.getHand());
        if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
            return InteractionResult.FAIL;
        }

        // Todo #0
        CraftEngineBlocks.place(placeLocation, blockStateToPlace, UpdateOption.UPDATE_ALL_IMMEDIATE, false);

        // TODO Make place event cancellable. Needs to get the previous block state from #0
        // TODO Add Bukkit block argument
        CustomBlockPlaceEvent placeEvent = new CustomBlockPlaceEvent((org.bukkit.entity.Player) player.platformPlayer(), placeLocation.clone(), blockStateToPlace, world.getBlockAt(placeLocation), context.getHand());
        EventUtils.fireAndForget(placeEvent);

        if (!player.isCreativeMode()) {
            Item<?> item = placeContext.getItem();
            item.count(item.count() - 1);
            item.load();
        }

        player.swingHand(placeContext.getHand());
        world.playSound(new Location(world, pos.x(), pos.y(), pos.z()), blockStateToPlace.sounds().placeSound().toString(), SoundCategory.BLOCKS, 1f, 1f);
        world.sendGameEvent((org.bukkit.entity.Player) player.platformPlayer(), GameEvent.BLOCK_PLACE, new Vector(pos.x(), pos.y(), pos.z()));
        return InteractionResult.SUCCESS;
    }

    @Nullable
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        return context;
    }

    protected ImmutableBlockState getPlacementState(BlockPlaceContext context, CustomBlock block) {
        ImmutableBlockState state = block.getStateForPlacement(context);
        return state != null && this.canPlace(context, state) ? state : null;
    }

    protected boolean checkStatePlacement() {
        return true;
    }

    protected boolean canPlace(BlockPlaceContext context, ImmutableBlockState state) {
        try {
            Object player;
            try {
                player = Reflections.method$CraftPlayer$getHandle.invoke(context.getPlayer().platformPlayer());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to get server player", e);
            }
            Object blockState = state.customBlockState().handle();
            Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());
            Object voxelShape = Reflections.method$CollisionContext$of.invoke(null, player);
            Object world = Reflections.field$CraftWorld$ServerLevel.get(context.getLevel().getHandle());
            boolean defaultReturn = ((!this.checkStatePlacement() || (boolean) Reflections.method$BlockStateBase$canSurvive.invoke(blockState, world, blockPos))
                    && (boolean) Reflections.method$ServerLevel$checkEntityCollision.invoke(world, blockState, player, voxelShape, blockPos, true));
            Block block = (Block) Reflections.method$CraftBlock$at.invoke(null, world, blockPos);
            BlockData blockData = (BlockData) Reflections.method$CraftBlockData$fromData.invoke(null, blockState);
            BlockCanBuildEvent canBuildEvent = new BlockCanBuildEvent(block, (org.bukkit.entity.Player) context.getPlayer().platformPlayer(), blockData, defaultReturn, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
            Bukkit.getPluginManager().callEvent(canBuildEvent);
            return canBuildEvent.isBuildable();
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to check canPlace", e);
            return false;
        }
    }

    public Key blockId() {
        return this.blockId;
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Key __, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new IllegalArgumentException("Missing required parameter 'block' for block_item behavior");
            }
            Key blockId = Key.of(id.toString());
            return new BlockItemBehavior(blockId);
        }
    }
}
