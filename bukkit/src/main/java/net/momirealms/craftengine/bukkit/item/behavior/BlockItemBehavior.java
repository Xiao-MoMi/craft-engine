package net.momirealms.craftengine.bukkit.item.behavior;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitBlockInWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.BlockBoundItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class BlockItemBehavior extends BlockBoundItemBehavior {
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
        Optional<CustomBlock> optionalBlock = BukkitBlockManager.instance().blockById(this.blockId);
        if (optionalBlock.isEmpty()) {
            CraftEngine.instance().logger().warn("Failed to place unknown block " + this.blockId);
            return InteractionResult.FAIL;
        }
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }

        CustomBlock block = optionalBlock.get();
        BlockPos pos = context.getClickedPos();
        int maxY = context.getLevel().worldHeight().getMaxBuildHeight() - 1;
        if (context.getClickedFace() == Direction.UP && pos.y() >= maxY) {
            context.getPlayer().sendActionBar(Component.translatable("build.tooHigh").arguments(Component.text(maxY)).color(NamedTextColor.RED));
            return InteractionResult.FAIL;
        }

        ImmutableBlockState blockStateToPlace = getPlacementState(context, block);
        if (blockStateToPlace == null) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        BlockPos againstPos = context.getAgainstPos();
        World world = (World) context.getLevel().platformWorld();
        Location placeLocation = new Location(world, pos.x(), pos.y(), pos.z());
        Block bukkitBlock = world.getBlockAt(placeLocation);
        Block againstBlock = world.getBlockAt(againstPos.x(), againstPos.y(), againstPos.z());
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.platformPlayer();

        if (player.isAdventureMode()) {
            Object againstBlockState = BlockStateUtils.blockDataToBlockState(againstBlock.getBlockData());
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(againstBlockState);
            if (optionalCustomState.isEmpty()) {
                if (!AdventureModeUtils.canPlace(context.getItem(), context.getLevel(), againstPos, againstBlockState)) {
                    return InteractionResult.FAIL;
                }
            } else {
                ImmutableBlockState customState = optionalCustomState.get();
                // custom block
                if (!AdventureModeUtils.canPlace(context.getItem(), context.getLevel(), againstPos, Config.simplifyAdventurePlaceCheck() ? customState.vanillaBlockState().handle() : againstBlockState)) {
                    return InteractionResult.FAIL;
                }
            }
        }

        // trigger event
        CustomBlockAttemptPlaceEvent attemptPlaceEvent = new CustomBlockAttemptPlaceEvent(bukkitPlayer, placeLocation.clone(), blockStateToPlace,
                DirectionUtils.toBlockFace(context.getClickedFace()), bukkitBlock, context.getHand());
        if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
            return InteractionResult.FAIL;
        }

        // it's just world + pos
        BlockState previousState = bukkitBlock.getState();
        // place custom block
        CraftEngineBlocks.place(placeLocation, blockStateToPlace, UpdateOption.UPDATE_ALL_IMMEDIATE, false);
        // call bukkit event
        BlockPlaceEvent bukkitPlaceEvent = new BlockPlaceEvent(bukkitBlock, previousState, againstBlock, (ItemStack) context.getItem().getItem(), bukkitPlayer, true, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
        if (EventUtils.fireAndCheckCancel(bukkitPlaceEvent)) {
            // revert changes
            previousState.update(true, false);
            return InteractionResult.FAIL;
        }

        // call custom event
        CustomBlockPlaceEvent customPlaceEvent = new CustomBlockPlaceEvent(bukkitPlayer, placeLocation.clone(), blockStateToPlace, world.getBlockAt(placeLocation), context.getHand());
        if (EventUtils.fireAndCheckCancel(customPlaceEvent)) {
            // revert changes
            previousState.update(true, false);
            return InteractionResult.FAIL;
        }

        WorldPosition position = new WorldPosition(context.getLevel(), pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Cancellable dummy = Cancellable.dummy();
        PlayerOptionalContext functionContext = PlayerOptionalContext.of(player, ContextHolder.builder()
                .withParameter(DirectContextParameters.BLOCK, new BukkitBlockInWorld(bukkitBlock))
                .withParameter(DirectContextParameters.POSITION, position)
                .withParameter(DirectContextParameters.EVENT, dummy)
                .withParameter(DirectContextParameters.HAND, context.getHand())
                .withParameter(DirectContextParameters.ITEM_IN_HAND, context.getItem())
        );
        block.execute(functionContext, EventTrigger.PLACE);
        if (dummy.isCancelled()) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }

        if (!player.isCreativeMode()) {
            Item<?> item = context.getItem();
            item.count(item.count() - 1);
        }

        block.setPlacedBy(context, blockStateToPlace);

        player.swingHand(context.getHand());
        context.getLevel().playBlockSound(position, blockStateToPlace.settings().sounds().placeSound());
        world.sendGameEvent(bukkitPlayer, GameEvent.BLOCK_PLACE, new Vector(pos.x(), pos.y(), pos.z()));
        return InteractionResult.SUCCESS;
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
            Object player = context.getPlayer().serverPlayer();
            Object blockState = state.customBlockState().handle();
            Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());
            Object voxelShape = CoreReflections.method$CollisionContext$of.invoke(null, player);
            Object world = FastNMS.INSTANCE.field$CraftWorld$ServerLevel((World) context.getLevel().platformWorld());
            boolean defaultReturn = ((!this.checkStatePlacement() || (boolean) CoreReflections.method$BlockStateBase$canSurvive.invoke(blockState, world, blockPos))
                    && (boolean) CoreReflections.method$ServerLevel$checkEntityCollision.invoke(world, blockState, player, voxelShape, blockPos, true));
            Block block = FastNMS.INSTANCE.method$CraftBlock$at(world, blockPos);
            BlockData blockData = FastNMS.INSTANCE.method$CraftBlockData$fromData(blockState);
            BlockCanBuildEvent canBuildEvent = new BlockCanBuildEvent(block, (org.bukkit.entity.Player) context.getPlayer().platformPlayer(), blockData, defaultReturn, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
            Bukkit.getPluginManager().callEvent(canBuildEvent);
            return canBuildEvent.isBuildable();
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to check canPlace", e);
            return false;
        }
    }

    @Override
    public Key block() {
        return this.blockId;
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Pack pack, Path path, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.block.missing_block", new IllegalArgumentException("Missing required parameter 'block' for block_item behavior"));
            }
            if (id instanceof Map<?, ?> map) {
                if (map.containsKey(key.toString())) {
                    // 防呆
                    BukkitBlockManager.instance().parser().parseSection(pack, path, key, MiscUtils.castToMap(map.get(key.toString()), false));
                } else {
                    BukkitBlockManager.instance().parser().parseSection(pack, path, key, MiscUtils.castToMap(map, false));
                }
                return new BlockItemBehavior(key);
            } else {
                return new BlockItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
