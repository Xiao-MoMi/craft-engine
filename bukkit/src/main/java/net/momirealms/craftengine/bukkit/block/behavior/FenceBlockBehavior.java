package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MTagKeys;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.IsPathFindableBlockBehavior;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.LeadItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.FenceGateBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.LeavesBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import org.bukkit.Location;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class FenceBlockBehavior extends BukkitBlockBehavior implements IsPathFindableBlockBehavior {
    public static final BlockBehaviorFactory<FenceBlockBehavior> FACTORY = new Factory();
    public static final Object InteractionResult$SUCCESS_SERVER = VersionHelper.isOrAbove1_21_2() ? InteractionResultProxy.INSTANCE.getSuccessServer() : InteractionResultProxy.INSTANCE.getSuccess();
    private final BooleanProperty northProperty;
    private final BooleanProperty eastProperty;
    private final BooleanProperty southProperty;
    private final BooleanProperty westProperty;
    private final Object connectableBlockTag;
    private final boolean canLeash;

    public FenceBlockBehavior(CustomBlock customBlock,
                              BooleanProperty northProperty,
                              BooleanProperty eastProperty,
                              BooleanProperty southProperty,
                              BooleanProperty westProperty,
                              Object connectableBlockTag,
                              boolean canLeash) {
        super(customBlock);
        this.northProperty = northProperty;
        this.eastProperty = eastProperty;
        this.southProperty = southProperty;
        this.westProperty = westProperty;
        this.connectableBlockTag = connectableBlockTag;
        this.canLeash = canLeash;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return false;
    }

    public boolean connectsTo(BlockStateWrapper state, boolean isSideSolid, HorizontalDirection direction) {
        boolean isSameFence = this.isSameFence(state);
        boolean flag = FenceGateBlockProxy.CLASS.isInstance(BlockStateUtils.getBlockOwner(state.literalObject()))
                ? FenceGateBlockProxy.INSTANCE.connectsToDirection(state.literalObject(), DirectionUtils.toNMSDirection(direction.toDirection()))
                : FenceGateBlockBehavior.connectsToDirection(state, direction);
        return !isExceptionForConnection(state) && isSideSolid || isSameFence || flag;
    }

    public static boolean isExceptionForConnection(BlockStateWrapper state) {
        Object blockState = state.literalObject();
        return LeavesBlockProxy.CLASS.isInstance(BlockStateUtils.getBlockOwner(blockState))
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, MBlocks.BARRIER)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, MBlocks.CARVED_PUMPKIN)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, MBlocks.JACK_O_LANTERN)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, MBlocks.MELON)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(blockState, MBlocks.PUMPKIN)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(blockState, MTagKeys.Block$SHULKER_BOXES);
    }

    private boolean isSameFence(BlockStateWrapper state) {
        Object blockState = state.literalObject();
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(blockState, MTagKeys.Block$FENCES)
                && BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(blockState, this.connectableBlockTag)
                == BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(this.customBlock.defaultState().customBlockState().literalObject(), this.connectableBlockTag);
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        if (!this.canLeash) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        World world = context.getLevel();
        Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.INTERACT, location)) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        Object interactionResult = LeadItemProxy.INSTANCE.bindPlayerMobs(player.serverPlayer(), context.getLevel().serverWorld(), LocationUtils.toBlockPos(pos));
        if (interactionResult == InteractionResult$SUCCESS_SERVER) {
            player.swingHand(InteractionHand.MAIN_HAND);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(level.serverWorld(), LocationUtils.toBlockPos(clickedPos));
        BlockPos blockPos = clickedPos.north();
        BlockPos blockPos1 = clickedPos.east();
        BlockPos blockPos2 = clickedPos.south();
        BlockPos blockPos3 = clickedPos.west();
        BlockStateWrapper blockState = level.getBlock(blockPos).blockState();
        BlockStateWrapper blockState1 = level.getBlock(blockPos1).blockState();
        BlockStateWrapper blockState2 = level.getBlock(blockPos2).blockState();
        BlockStateWrapper blockState3 = level.getBlock(blockPos3).blockState();
        BooleanProperty waterlogged = (BooleanProperty) state.owner().value().getProperty("waterlogged");
        if (waterlogged != null) {
            state = state.with(waterlogged, FluidStateProxy.INSTANCE.getType(fluidState) == MFluids.WATER);
        }
        return state
                .with(this.northProperty, this.connectsTo(blockState, BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(blockState.literalObject(), level.serverWorld(), LocationUtils.toBlockPos(blockPos), DirectionProxy.SOUTH, SupportTypeProxy.FULL), HorizontalDirection.SOUTH))
                .with(this.eastProperty, this.connectsTo(blockState1, BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(blockState1.literalObject(), level.serverWorld(), LocationUtils.toBlockPos(blockPos1), DirectionProxy.WEST, SupportTypeProxy.FULL), HorizontalDirection.WEST))
                .with(this.southProperty, this.connectsTo(blockState2, BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(blockState2.literalObject(), level.serverWorld(), LocationUtils.toBlockPos(blockPos2), DirectionProxy.NORTH, SupportTypeProxy.FULL), HorizontalDirection.NORTH))
                .with(this.westProperty, this.connectsTo(blockState3, BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(blockState3.literalObject(), level.serverWorld(), LocationUtils.toBlockPos(blockPos3), DirectionProxy.EAST, SupportTypeProxy.FULL), HorizontalDirection.EAST));
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Optional<ImmutableBlockState> optionalState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
        BooleanProperty waterlogged = (BooleanProperty) optionalState
                .map(ImmutableBlockState::owner)
                .map(Holder::value)
                .map(block -> block.getProperty("waterlogged"))
                .orElse(null);
        if (waterlogged != null) {
            LevelUtils.scheduleFluidTick(args[updateShape$level], args[updateShape$blockPos], MFluids.WATER, 5);
        }
        if (DirectionUtils.fromNMSDirection(args[updateShape$direction]).axis().isHorizontal() && optionalState.isPresent()) {
            Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]);
            ImmutableBlockState state = optionalState.get();
            if (state.owner() != null) {
                BooleanProperty booleanProperty = (BooleanProperty) state.owner().value().getProperty(direction.name().toLowerCase(Locale.ROOT));
                if (booleanProperty != null) {
                    BlockStateWrapper wrapper = BlockStateUtils.toBlockStateWrapper(args[updateShape$neighborState]);
                    return state.with(booleanProperty, this.connectsTo(wrapper, BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(wrapper.literalObject(), args[updateShape$level], args[5], DirectionUtils.toNMSDirection(direction.opposite()), SupportTypeProxy.FULL), direction.opposite().toHorizontalDirection())).customBlockState().literalObject();
                }
            }
        }
        return superMethod.call();
    }

    private static class Factory implements BlockBehaviorFactory<FenceBlockBehavior> {

        @Override
        public FenceBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            BooleanProperty north = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("north"), "warning.config.block.behavior.fence.missing_north");
            BooleanProperty east = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("east"), "warning.config.block.behavior.fence.missing_east");
            BooleanProperty south = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("south"), "warning.config.block.behavior.fence.missing_south");
            BooleanProperty west = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("west"), "warning.config.block.behavior.fence.missing_west");
            Object connectableBlockTag = TagKeyProxy.INSTANCE.create(RegistriesProxy.BLOCK, KeyUtils.toIdentifier(Key.of(arguments.getOrDefault("connectable-block-tag", "minecraft:wooden_fences").toString())));
            connectableBlockTag = connectableBlockTag != null ? connectableBlockTag : MTagKeys.Block$WOODEN_FENCES;
            boolean canLeash = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-leash", false), "can-leash");
            return new FenceBlockBehavior(block, north, east, south, west, connectableBlockTag, canLeash);
        }
    }
}
